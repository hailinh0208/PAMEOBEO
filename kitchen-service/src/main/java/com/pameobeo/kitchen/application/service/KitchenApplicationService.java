package com.pameobeo.kitchen.application.service;

import com.pameobeo.kitchen.application.port.in.AvailabilityQuery;
import com.pameobeo.kitchen.application.port.in.KitchenAdminUseCase;
import com.pameobeo.kitchen.application.port.in.MenuQuery;
import com.pameobeo.kitchen.application.port.in.QuoteOrderUseCase;
import com.pameobeo.kitchen.application.port.in.ReleaseSlotUseCase;
import com.pameobeo.kitchen.application.port.in.ReserveSlotUseCase;
import com.pameobeo.kitchen.application.port.out.CookingDayRepository;
import com.pameobeo.kitchen.application.port.out.KitchenEventPublisher;
import com.pameobeo.kitchen.application.port.out.MenuItemImageRepository;
import com.pameobeo.kitchen.application.port.out.MenuItemRepository;
import com.pameobeo.kitchen.application.port.out.ProcessedEventStore;
import com.pameobeo.kitchen.application.port.out.SlotReservationRepository;
import com.pameobeo.kitchen.domain.exception.CookingDayFullException;
import com.pameobeo.kitchen.domain.exception.ImageNotFoundException;
import com.pameobeo.kitchen.domain.exception.MenuItemNotFoundException;
import com.pameobeo.kitchen.domain.exception.OrderAfterCutoffException;
import com.pameobeo.kitchen.domain.model.CookingDay;
import com.pameobeo.kitchen.domain.model.CutoffPolicy;
import com.pameobeo.kitchen.domain.model.Ingredient;
import com.pameobeo.kitchen.domain.model.MenuHighlights;
import com.pameobeo.kitchen.domain.model.MenuItem;
import com.pameobeo.kitchen.domain.model.MenuItemImage;
import com.pameobeo.kitchen.domain.model.SlotReservation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class KitchenApplicationService implements MenuQuery, AvailabilityQuery, QuoteOrderUseCase,
        ReserveSlotUseCase, ReleaseSlotUseCase, KitchenAdminUseCase {

    private static final Logger log = LoggerFactory.getLogger(KitchenApplicationService.class);

    private final CookingDayRepository cookingDays;
    private final SlotReservationRepository reservations;
    private final MenuItemRepository menu;
    private final MenuItemImageRepository images;
    private final ProcessedEventStore processedEvents;
    private final KitchenEventPublisher events;
    private final CutoffPolicy cutoffPolicy;
    private final int defaultCapacity;
    private final Clock clock;

    public KitchenApplicationService(CookingDayRepository cookingDays, SlotReservationRepository reservations,
                                     MenuItemRepository menu, MenuItemImageRepository images,
                                     ProcessedEventStore processedEvents,
                                     KitchenEventPublisher events, CutoffPolicy cutoffPolicy,
                                     int defaultCapacity, Clock clock) {
        this.cookingDays = cookingDays;
        this.reservations = reservations;
        this.menu = menu;
        this.images = images;
        this.processedEvents = processedEvents;
        this.events = events;
        this.cutoffPolicy = cutoffPolicy;
        this.defaultCapacity = defaultCapacity;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- queries

    @Override
    @Transactional(readOnly = true)
    public List<MenuEntry> activeMenu() {
        return entries(menu.findAll().stream().filter(MenuItem::isActive).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HighlightEntry> highlights() {
        Map<UUID, Integer> sold = jarsSold();
        Map<UUID, List<UUID>> imageIds = images.imageIdsByMenuItem();
        return MenuHighlights.pick(menu.findAll(), sold, HIGHLIGHTS).stream()
                .map(h -> new HighlightEntry(new MenuEntry(h.item(), h.jarsSold(),
                        imageIds.getOrDefault(h.item().getId(), List.of())), h.reason()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MenuItemImage> image(UUID menuItemId, UUID imageId) {
        return images.find(menuItemId, imageId);
    }

    private Map<UUID, Integer> jarsSold() {
        return reservations.jarsSoldSince(clock.instant().minus(Duration.ofDays(SALES_WINDOW_DAYS)));
    }

    private List<MenuEntry> entries(List<MenuItem> items) {
        Map<UUID, Integer> sold = jarsSold();
        Map<UUID, List<UUID>> imageIds = images.imageIdsByMenuItem();
        return items.stream()
                .map(m -> new MenuEntry(m, sold.getOrDefault(m.getId(), 0), imageIds.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Availability nextCookingDay() {
        return forDate(cutoffPolicy.cookingDateFor(clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public Availability forDate(LocalDate cookingDate) {
        return toAvailability(loadOrOpen(cookingDate));
    }

    @Override
    @Transactional(readOnly = true)
    public Quote quote(Instant orderedAt, List<UUID> menuItemIds) {
        LocalDate cookingDate = cutoffPolicy.cookingDateFor(orderedAt);
        CookingDay day = loadOrOpen(cookingDate);
        Map<UUID, MenuItem> found = menu.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        List<QuotedItem> items = menuItemIds.stream().distinct().map(id -> {
            MenuItem item = found.get(id);
            if (item == null) {
                throw new MenuItemNotFoundException(id);
            }
            return new QuotedItem(id, item.getName(), item.getPriceVnd(), item.isActive());
        }).toList();

        return new Quote(cookingDate, cutoffPolicy.cutoffFor(cookingDate), cutoffPolicy.isAfterTodayCutoff(orderedAt),
                day.remaining(), items);
    }

    // ---------------------------------------------------------------- saga participant

    /**
     * Runs in one transaction: processed-event marker + cooking day update + reservation + outbox row.
     * If another consumer updated the same cooking day concurrently, the optimistic lock fails, the
     * whole transaction rolls back and Kafka redelivers the event (retry).
     */
    @Override
    @Transactional
    public void reserve(ReserveSlotCommand cmd) {
        if (!processedEvents.markProcessed(cmd.eventId())) {
            log.info("Duplicate OrderCreated {} for order {} ignored", cmd.eventId(), cmd.orderId());
            return;
        }
        if (reservations.findByOrderId(cmd.orderId()).isPresent()) {
            log.info("Order {} already has a reservation, ignored", cmd.orderId());
            return;
        }

        CookingDay day = loadOrOpen(cmd.cookingDate());
        try {
            day.reserve(cmd.quantity(), cmd.orderedAt(), cutoffPolicy);
        } catch (CookingDayFullException | OrderAfterCutoffException e) {
            log.info("Order {} rejected for {}: {}", cmd.orderId(), cmd.cookingDate(), e.getCode());
            events.slotFull(cmd.orderId(), cmd.cookingDate(), cmd.quantity(), day.remaining(), e.getCode());
            return;
        }

        CookingDay saved = cookingDays.save(day);
        reservations.save(SlotReservation.reserve(cmd.orderId(), cmd.cookingDate(), cmd.quantity(), cmd.lines(),
                clock.instant()));
        events.slotReserved(cmd.orderId(), cmd.cookingDate(), cmd.quantity(), saved.remaining());
        log.info("Reserved {} jars for order {} on {} ({} left)",
                cmd.quantity(), cmd.orderId(), cmd.cookingDate(), saved.remaining());
    }

    @Override
    @Transactional
    public void release(UUID eventId, UUID orderId) {
        if (!processedEvents.markProcessed(eventId)) {
            return;
        }
        reservations.findByOrderId(orderId).ifPresent(reservation -> {
            if (!reservation.release()) {
                return;
            }
            CookingDay day = loadOrOpen(reservation.getCookingDate());
            day.release(reservation.getQuantity());
            CookingDay saved = cookingDays.save(day);
            reservations.save(reservation);
            events.slotReleased(orderId, reservation.getCookingDate(), reservation.getQuantity(), saved.remaining());
            log.info("Released {} jars of order {} on {}", reservation.getQuantity(), orderId, reservation.getCookingDate());
        });
    }

    // ---------------------------------------------------------------- admin

    @Override
    @Transactional
    public Availability changeCapacity(LocalDate cookingDate, int capacity) {
        CookingDay day = loadOrOpen(cookingDate);
        day.changeCapacity(capacity);
        return toAvailability(cookingDays.save(day));
    }

    @Override
    @Transactional
    public MenuEntry updateMenuItem(UUID id, Long priceVnd, Boolean active, Boolean featured) {
        MenuItem item = menu.findById(id).orElseThrow(() -> new MenuItemNotFoundException(id));
        if (priceVnd != null) {
            item.changePrice(priceVnd);
        }
        if (active != null) {
            item.setActive(active);
        }
        if (featured != null) {
            item.setFeatured(featured);
        }
        return entries(List.of(menu.save(item))).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuEntry> fullMenu() {
        return entries(menu.findAll());
    }

    @Override
    @Transactional
    public List<UUID> addImage(UUID menuItemId, byte[] data) {
        menu.findById(menuItemId).orElseThrow(() -> new MenuItemNotFoundException(menuItemId));
        MenuItemImage image = MenuItemImage.upload(menuItemId, images.imageIdsOf(menuItemId).size(), data,
                clock.instant());
        images.save(image);
        log.info("Image {} ({}, {} bytes) added to menu item {}", image.id(), image.contentType(), data.length, menuItemId);
        return images.imageIdsOf(menuItemId);
    }

    @Override
    @Transactional
    public List<UUID> deleteImage(UUID menuItemId, UUID imageId) {
        images.find(menuItemId, imageId).orElseThrow(() -> new ImageNotFoundException(imageId));
        images.delete(imageId);
        List<UUID> remaining = images.imageIdsOf(menuItemId);
        images.reorder(remaining); // no gap: the next photo becomes the cover
        return remaining;
    }

    @Override
    @Transactional
    public List<UUID> makeCover(UUID menuItemId, UUID imageId) {
        List<UUID> ids = new ArrayList<>(images.imageIdsOf(menuItemId));
        if (!ids.remove(imageId)) {
            throw new ImageNotFoundException(imageId);
        }
        ids.add(0, imageId);
        images.reorder(ids);
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public CookingPlan cookingPlan(LocalDate cookingDate) {
        List<SlotReservation> held = reservations.findReservedOn(cookingDate);
        Map<UUID, MenuItem> items = menu.findAll().stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        Map<MenuItem, Integer> jarsPerItem = new TreeMap<>(MenuItem.DISPLAY_ORDER);
        int reservedJars = 0;
        int jarsWithoutDetails = 0;
        for (SlotReservation r : held) {
            reservedJars += r.getQuantity();
            if (r.getLines().isEmpty()) {
                jarsWithoutDetails += r.getQuantity();
            }
            for (SlotReservation.Line line : r.getLines()) {
                MenuItem item = items.get(line.menuItemId());
                if (item == null) {
                    jarsWithoutDetails += line.quantity();
                } else {
                    jarsPerItem.merge(item, line.quantity(), Integer::sum);
                }
            }
        }

        Map<Ingredient, Integer> grams = new EnumMap<>(Ingredient.class);
        jarsPerItem.forEach((item, jars) ->
                item.recipe().grams().forEach((ingredient, g) -> grams.merge(ingredient, g * jars, Integer::sum)));

        return new CookingPlan(cookingDate, reservedJars, jarsWithoutDetails,
                jarsPerItem.entrySet().stream()
                        .map(e -> new PlannedItem(e.getKey().getId(), e.getKey().getName(), e.getValue()))
                        .toList(),
                grams.entrySet().stream()
                        .map(e -> new IngredientTotal(e.getKey(), e.getKey().label(), e.getValue()))
                        .toList());
    }

    private CookingDay loadOrOpen(LocalDate date) {
        return cookingDays.findByDate(date).orElseGet(() -> CookingDay.open(date, defaultCapacity));
    }

    private Availability toAvailability(CookingDay day) {
        return new Availability(day.getDate(), cutoffPolicy.cutoffFor(day.getDate()),
                day.getCapacity(), day.getReservedCount(), day.remaining());
    }
}
