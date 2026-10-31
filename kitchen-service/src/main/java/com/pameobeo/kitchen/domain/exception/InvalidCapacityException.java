package com.pameobeo.kitchen.domain.exception;

public class InvalidCapacityException extends DomainException {
    public InvalidCapacityException(int requested, int reserved) {
        super("INVALID_CAPACITY", "Capacity " + requested + " is below the " + reserved + " jars already reserved");
    }
}
