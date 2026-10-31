package com.pameobeo.notification.application.port.in;

import com.pameobeo.notification.domain.model.OrderStatusChange;
import java.util.UUID;

public interface NotifyOrderStatusUseCase {

    void notify(UUID eventId, OrderStatusChange change);
}
