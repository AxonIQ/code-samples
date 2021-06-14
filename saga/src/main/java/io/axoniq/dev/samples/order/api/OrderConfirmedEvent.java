package io.axoniq.dev.samples.order.api;

import io.axoniq.dev.samples.uuid.OrderId;

public class OrderConfirmedEvent {

    OrderId orderId;

    public OrderConfirmedEvent(OrderId orderId) {
        this.orderId = orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }
}
