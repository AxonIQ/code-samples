package io.axoniq.demo.orderfulfillment.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record OrderDelivered(
        String orderId,
        String trackingNumber
) {
}
