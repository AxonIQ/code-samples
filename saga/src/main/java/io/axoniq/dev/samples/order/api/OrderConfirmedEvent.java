package io.axoniq.dev.samples.order.api;

import io.axoniq.dev.samples.uuid.OrderId;

public record OrderConfirmedEvent(OrderId orderId) {

}

