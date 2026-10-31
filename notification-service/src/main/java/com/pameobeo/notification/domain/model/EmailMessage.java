package com.pameobeo.notification.domain.model;

import java.util.UUID;

public record EmailMessage(UUID orderId, String to, String subject, String body, Recipient recipient) {

    public enum Recipient { CUSTOMER, SHOP }
}
