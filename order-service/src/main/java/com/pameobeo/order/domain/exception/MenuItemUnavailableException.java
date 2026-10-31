package com.pameobeo.order.domain.exception;

public class MenuItemUnavailableException extends DomainException {
    public MenuItemUnavailableException(String name) {
        super("MENU_ITEM_UNAVAILABLE", "'" + name + "' is no longer on sale");
    }
}
