import { Component, DestroyRef, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { apiError, errorText } from '../core/errors';
import { Availability, Highlight, Meat, MenuItem, PublicMenuItem } from '../core/models';
import { ToastService } from '../core/toast.service';
import { ProductPhoto } from '../shared/product-photo';
import {
  CookingDayPipe, MEATS, MEAT_LABEL, PATE_TYPES, PateType, VndPipe, ingredientsText, meatEmoji, newIdempotencyKey,
} from '../shared/format';

const MAX_JARS = 10;
const ADDRESS_KEY = 'pameobeo.address';

/** UC02: menu + remaining slots + cart + place order. */
@Component({
  selector: 'app-shop',
  imports: [FormsModule, RouterLink, VndPipe, CookingDayPipe, ProductPhoto],
  template: `
    @if (availability(); as a) {
      <section class="hero card">
        <div>
          <p class="eyebrow">Đặt hôm nay · giao {{ a.cookingDate | cookingDay }}, 18:00–22:00</p>
          <h1>Pate tươi nấu theo đơn 🐾</h1>
          <p class="muted">
            Chốt đơn lúc <strong>18:00</strong> · còn <strong>{{ countdown() }}</strong>.
            Bếp chỉ nấu tối đa {{ a.capacity }} hộp mỗi ngày.
          </p>
        </div>
        <div class="slots">
          <div class="slots-number" [class.low]="a.remaining <= 5">{{ a.remaining }}</div>
          <div class="muted">suất còn lại</div>
          <div class="progress"><span [style.width.%]="(a.reserved / (a.capacity || 1)) * 100"></span></div>
          <div class="muted small">{{ a.reserved }}/{{ a.capacity }} hộp đã đặt</div>
        </div>
      </section>
    }

    @if (highlights().length) {
      <section class="highlights" (mouseenter)="paused = true" (mouseleave)="paused = false"
               (focusin)="paused = true" (focusout)="paused = false">
        <div class="page-header">
          <h2>{{ hasBestSellers() ? '🔥 Bán chạy' : '⭐ Gợi ý của tiệm' }}</h2>
          <div class="row">
            <button class="btn ghost small" (click)="slide(-1)" aria-label="Trước">‹</button>
            <button class="btn ghost small" (click)="slide(1)" aria-label="Sau">›</button>
          </div>
        </div>
        <div class="carousel" #track>
          @for (h of highlights(); track h.item.id) {
            <article class="card highlight-card">
              @if (h.reason === 'BEST_SELLER') {
                <span class="badge badge-hot">🔥 Bán chạy</span>
              } @else {
                <span class="badge badge-pick">⭐ Gợi ý của tiệm</span>
              }
              <app-product-photo [item]="h.item" />
              <h3>{{ h.item.name }}</h3>
              <p class="muted small">{{ ingredientsText(h.item) }}</p>
              <p class="price">{{ h.item.priceVnd | vnd }}</p>
              @if (h.item.jarsSold > 0) {
                <p class="muted small">Đã bán {{ h.item.jarsSold }} hộp trong 30 ngày</p>
              }
              <button class="btn primary small" (click)="addOne(h.item)" [disabled]="totalJars() >= maxJars">
                + Thêm vào giỏ
              </button>
            </article>
          }
        </div>
      </section>
    }

    <div class="shop-grid">
      <section>
        <h2>1. Chọn loại pate</h2>
        <p class="muted small">Mỗi hộp 400g, nấu tươi theo đơn.</p>
        <div class="menu">
          @for (t of types; track t.key) {
            <button class="card menu-item type-card" [class.selected]="type().key === t.key" (click)="selectType(t)">
              <h3>{{ t.label }}</h3>
              <p class="muted small">{{ typeRecipe(t) }}</p>
              @if (fromPrice(t); as p) {
                <p class="price">từ {{ p | vnd }}</p>
              } @else {
                <p class="muted">Tạm ngừng bán</p>
              }
            </button>
          } @empty {
            <p class="muted">Đang tải menu…</p>
          }
        </div>

        <h2>2. Chọn {{ type().mix ? '2 vị' : 'vị' }}</h2>
        <div class="row">
          @for (m of meats; track m) {
            <button class="btn chip" [class.selected]="picked().includes(m)" (click)="pick(m)"
                    [disabled]="!type().mix && !findItem(type(), [m])?.active">
              {{ meatEmoji(m) }} {{ meatLabel[m] }}
            </button>
          }
        </div>

        <article class="card choice">
          @if (selected(); as item) {
            <app-product-photo [item]="item" />
            <div class="choice-body">
              <h3>{{ item.name }}</h3>
              <p class="muted small">{{ ingredientsText(item) }}</p>
              @if (item.jarsSold > 0) {
                <p class="muted small">Đã bán {{ item.jarsSold }} hộp trong 30 ngày</p>
              }
              @if (item.active) {
                <p class="price">{{ item.priceVnd | vnd }} <span class="muted">/ hộp</span></p>
                <div class="row">
                  <div class="stepper">
                    <button class="btn ghost" (click)="pickQty.set(pickQty() - 1)" [disabled]="pickQty() <= 1">−</button>
                    <span>{{ pickQty() }}</span>
                    <button class="btn ghost" (click)="pickQty.set(pickQty() + 1)"
                            [disabled]="totalJars() + pickQty() >= maxJars">+</button>
                  </div>
                  <button class="btn primary" (click)="addSelected(item)" [disabled]="totalJars() + pickQty() > maxJars">
                    Thêm vào giỏ
                  </button>
                </div>
              } @else {
                <p class="warning">Vị này đang tạm ngừng bán.</p>
              }
            </div>
          } @else {
            <p class="muted">{{ type().mix ? 'Chọn 2 vị khác nhau để xem giá.' : 'Chọn 1 vị để xem giá.' }}</p>
          }
        </article>
      </section>

      <aside class="card cart">
        <h2>Giỏ hàng</h2>
        @for (line of cartLines(); track line.item.id) {
          <div class="cart-line">
            <span class="cart-name"><app-product-photo [item]="line.item" size="thumb" /> {{ line.item.name }}</span>
            <span class="stepper small-stepper">
              <button class="btn ghost small" (click)="change(line.item, -1)">−</button>
              <span>{{ line.qty }}</span>
              <button class="btn ghost small" (click)="change(line.item, 1)" [disabled]="totalJars() >= maxJars">+</button>
            </span>
            <span>{{ line.item.priceVnd * line.qty | vnd }}</span>
          </div>
        } @empty {
          <p class="muted">Chưa có món nào. Chọn pate ở bên trái nhé.</p>
        }
        <div class="cart-total">
          <span>Tổng ({{ totalJars() }}/{{ maxJars }} hộp)</span>
          <strong>{{ totalVnd() | vnd }}</strong>
        </div>
        @if (availability(); as a) {
          @if (totalJars() > a.remaining) {
            <p class="warning">Chỉ còn {{ a.remaining }} suất cho ngày nấu này.</p>
          }
        }
        <label>Địa chỉ giao hàng
          <textarea rows="2" [(ngModel)]="address" placeholder="Số nhà, đường, quận…"></textarea>
        </label>
        <p class="muted small">Giỏ hàng không giữ suất — suất chỉ được giữ khi bếp xác nhận đơn.</p>
        @if (!auth.isLoggedIn()) {
          <a class="btn primary block" routerLink="/login" [queryParams]="{ returnUrl: '/' }">Đăng nhập để đặt hàng</a>
        } @else {
          <button class="btn primary block" (click)="placeOrder(false)"
                  [disabled]="placing() || totalJars() === 0 || !address.trim()">
            {{ placing() ? 'Đang gửi đơn…' : 'Đặt hàng' }}
          </button>
        }
        @if (error()) { <p class="error">{{ error() }}</p> }
      </aside>
    </div>
  `,
})
export class ShopPage {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly maxJars = MAX_JARS;
  protected readonly types = PATE_TYPES;
  protected readonly meats = MEATS;
  protected readonly meatLabel = MEAT_LABEL;
  protected readonly meatEmoji = meatEmoji;
  protected readonly ingredientsText = ingredientsText;
  protected readonly menu = signal<PublicMenuItem[]>([]);
  protected readonly highlights = signal<Highlight[]>([]);
  protected readonly hasBestSellers = computed(() => this.highlights().some((h) => h.reason === 'BEST_SELLER'));
  /** The carousel stops sliding on its own while the customer hovers or focuses it. */
  protected paused = false;
  private readonly track = viewChild<ElementRef<HTMLElement>>('track');
  protected readonly type = signal<PateType>(PATE_TYPES[0]);
  protected readonly picked = signal<Meat[]>([]);
  protected readonly pickQty = signal(1);
  /** The menu item matching the chosen type and flavors, once enough flavors are picked. */
  protected readonly selected = computed(() => {
    const t = this.type();
    return this.picked().length === (t.mix ? 2 : 1) ? this.findItem(t, this.picked()) ?? null : null;
  });
  protected readonly availability = signal<Availability | null>(null);
  protected readonly cart = signal<Record<string, number>>({});
  protected readonly placing = signal(false);
  protected readonly error = signal('');
  protected readonly now = signal(Date.now());
  protected address = readAddress();
  private idempotencyKey = newIdempotencyKey();

  protected readonly cartLines = computed(() =>
    this.menu().filter((m) => (this.cart()[m.id] ?? 0) > 0).map((item) => ({ item, qty: this.cart()[item.id] })));
  protected readonly totalJars = computed(() => this.cartLines().reduce((sum, l) => sum + l.qty, 0));
  protected readonly totalVnd = computed(() => this.cartLines().reduce((sum, l) => sum + l.qty * l.item.priceVnd, 0));
  protected readonly countdown = computed(() => {
    const a = this.availability();
    if (!a) return '';
    const ms = new Date(a.orderCutoffAt).getTime() - this.now();
    if (ms <= 0) return 'đã qua giờ chốt';
    const h = Math.floor(ms / 3_600_000);
    const m = Math.floor((ms % 3_600_000) / 60_000);
    return h > 0 ? `${h} giờ ${m} phút` : `${m} phút`;
  });

  constructor() {
    this.api.menu().subscribe({ next: (m) => this.menu.set(m), error: (e) => this.error.set(errorText(e)) });
    this.loadAvailability();
    this.api.highlights().subscribe({ next: (h) => this.highlights.set(h), error: () => undefined });
    const timer = setInterval(() => this.now.set(Date.now()), 30_000);
    const refresh = setInterval(() => this.loadAvailability(), 15_000);
    const autoplay = setInterval(() => { if (!this.paused) this.slide(1); }, 4_000);
    inject(DestroyRef).onDestroy(() => { clearInterval(timer); clearInterval(refresh); clearInterval(autoplay); });
  }

  /** Scrolls the carousel by one card; past the last card it goes back to the first one (and vice versa). */
  protected slide(direction: 1 | -1): void {
    const el = this.track()?.nativeElement;
    const card = el?.firstElementChild as HTMLElement | null;
    if (!el || !card) return;
    const step = card.offsetWidth + 16; // card + gap
    const max = el.scrollWidth - el.clientWidth;
    if (max <= 0) return; // every card already fits on screen
    const atEnd = el.scrollLeft >= max - 4;
    const atStart = el.scrollLeft <= 4;
    const left = direction === 1 ? (atEnd ? 0 : el.scrollLeft + step) : (atStart ? max : el.scrollLeft - step);
    el.scrollTo({ left, behavior: 'smooth' });
  }

  protected addOne(item: PublicMenuItem): void {
    this.change(item, 1);
    this.toast.success(`Đã thêm 1 hộp ${item.name}`);
  }

  protected selectType(t: PateType): void {
    this.type.set(t);
    // keep the flavors that still make sense for the new type
    this.picked.update((p) => (t.mix ? p : p.slice(-1)));
    this.pickQty.set(1);
  }

  /** Single: picks that flavor. Mix: toggles it, keeping at most 2 (the oldest pick drops out). */
  protected pick(m: Meat): void {
    this.picked.update((p) => {
      if (!this.type().mix) return [m];
      if (p.includes(m)) return p.filter((x) => x !== m);
      return [...p, m].slice(-2);
    });
    this.pickQty.set(1);
  }

  protected findItem(t: PateType, meats: Meat[]): PublicMenuItem | undefined {
    const key = [...meats].sort().join();
    return this.menu().find((i) => i.mix === t.mix && i.withLiver === t.withLiver && [...i.meats].sort().join() === key);
  }

  protected fromPrice(t: PateType): number | null {
    const prices = this.menu().filter((i) => i.mix === t.mix && i.withLiver === t.withLiver && i.active)
      .map((i) => i.priceVnd);
    return prices.length ? Math.min(...prices) : null;
  }

  /** "Thịt, gan, cà rốt, bí đỏ" / "2 loại thịt, cà rốt, bí đỏ": ingredient names only, no quantities */
  protected typeRecipe(t: PateType): string {
    const sample = this.menu().find((i) => i.mix === t.mix && i.withLiver === t.withLiver);
    if (!sample) return '';
    // ingredients come in recipe order: the meat(s) first, then liver / vegetables
    const rest = sample.ingredients.slice(sample.meats.length).map((x) => x.toLowerCase());
    return [t.mix ? '2 loại thịt' : 'Thịt', ...rest].join(', ');
  }

  protected addSelected(item: MenuItem): void {
    this.change(item, this.pickQty());
    this.toast.success(`Đã thêm ${this.pickQty()} hộp ${item.name}`);
    this.pickQty.set(1);
  }

  protected change(item: MenuItem, delta: number): void {
    this.cart.update((c) => ({ ...c, [item.id]: Math.max(0, (c[item.id] ?? 0) + delta) }));
  }

  /** confirmNextCookingDay = true is sent only after the customer accepted the later date (UC02 - Ex2). */
  protected placeOrder(confirmNextCookingDay: boolean): void {
    this.placing.set(true);
    this.error.set('');
    try { localStorage.setItem(ADDRESS_KEY, this.address); } catch { /* ignore */ }

    const body = {
      items: this.cartLines().map((l) => ({ menuItemId: l.item.id, quantity: l.qty })),
      deliveryAddress: this.address.trim(),
      confirmNextCookingDay,
    };
    this.api.placeOrder(body, this.idempotencyKey).subscribe({
      next: (order) => {
        this.idempotencyKey = newIdempotencyKey();
        this.cart.set({});
        this.toast.success('Đã gửi đơn! Bếp đang xác nhận…');
        this.router.navigate(['/orders', order.id]);
      },
      error: (err) => {
        this.placing.set(false);
        const e = apiError(err);
        if (e.code === 'CUTOFF_PASSED' && e.suggestedCookingDate
            && confirm(`Đã qua giờ chốt 18:00. Đơn sẽ được nấu và giao vào ngày ${e.suggestedCookingDate}. Bạn đồng ý chứ?`)) {
          this.placeOrder(true);
          return;
        }
        this.error.set(errorText(err));
        this.loadAvailability();
      },
    });
  }

  private loadAvailability(): void {
    this.api.availability().subscribe({ next: (a) => this.availability.set(a), error: () => undefined });
  }
}

function readAddress(): string {
  try { return localStorage.getItem(ADDRESS_KEY) ?? ''; } catch { return ''; }
}
