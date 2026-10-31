package com.pameobeo.kitchen.application.port.out;

import com.pameobeo.kitchen.domain.model.MenuItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository {

    List<MenuItem> findAll();

    List<MenuItem> findAllById(Collection<UUID> ids);

    Optional<MenuItem> findById(UUID id);

    MenuItem save(MenuItem item);
}
