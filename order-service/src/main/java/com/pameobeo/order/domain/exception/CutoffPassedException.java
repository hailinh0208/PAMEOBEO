package com.pameobeo.order.domain.exception;

import java.time.LocalDate;

/** UC02 - Ex2: ask the customer to confirm that the order moves to a later cooking day. */
public class CutoffPassedException extends DomainException {

    private final LocalDate suggestedCookingDate;

    public CutoffPassedException(LocalDate suggestedCookingDate) {
        super("CUTOFF_PASSED", "Today's cut-off (18:00) has passed. The order would be cooked and delivered on "
                + suggestedCookingDate + ". Resend with confirmNextCookingDay=true to accept.");
        this.suggestedCookingDate = suggestedCookingDate;
    }

    public LocalDate getSuggestedCookingDate() {
        return suggestedCookingDate;
    }
}
