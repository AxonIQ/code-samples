package io.axoniq.demo.orderfulfillment.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record OrderPlaced(
        String orderId,
        String customerId,
        String email,
        double amount
) {
}
