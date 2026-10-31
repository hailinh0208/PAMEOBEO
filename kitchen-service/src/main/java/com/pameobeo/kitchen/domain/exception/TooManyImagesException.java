package com.pameobeo.kitchen.domain.exception;

public class TooManyImagesException extends DomainException {
    public TooManyImagesException(int max) {
        super("TOO_MANY_IMAGES", "A menu item has at most " + max + " images");
    }
}
