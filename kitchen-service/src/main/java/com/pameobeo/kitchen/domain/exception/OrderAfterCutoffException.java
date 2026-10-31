package com.pameobeo.kitchen.domain.exception;

import java.time.LocalDate;

public class OrderAfterCutoffException extends DomainException {
    public OrderAfterCutoffException(LocalDate cookingDate) {
        super("CUTOFF_PASSED", "Orders for cooking day " + cookingDate + " are closed");
    }
}
