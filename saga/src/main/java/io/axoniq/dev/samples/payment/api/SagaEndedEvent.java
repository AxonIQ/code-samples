package io.axoniq.dev.samples.payment.api;

import io.axoniq.dev.samples.uuid.OrderId;

public class SagaEndedEvent {

    OrderId orderId;

    public SagaEndedEvent(OrderId orderId) {
        this.orderId = orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }
}
