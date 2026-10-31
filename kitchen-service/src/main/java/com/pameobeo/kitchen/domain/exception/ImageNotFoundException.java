package com.pameobeo.kitchen.domain.exception;

import java.util.UUID;

public class ImageNotFoundException extends DomainException {
    public ImageNotFoundException(UUID id) {
        super("IMAGE_NOT_FOUND", "Image " + id + " does not exist");
    }
}
