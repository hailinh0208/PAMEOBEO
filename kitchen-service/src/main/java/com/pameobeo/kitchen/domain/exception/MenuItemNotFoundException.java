package com.pameobeo.kitchen.domain.exception;

import java.util.UUID;

public class MenuItemNotFoundException extends DomainException {
    public MenuItemNotFoundException(UUID id) {
        super("MENU_ITEM_NOT_FOUND", "Menu item " + id + " does not exist");
    }
}
