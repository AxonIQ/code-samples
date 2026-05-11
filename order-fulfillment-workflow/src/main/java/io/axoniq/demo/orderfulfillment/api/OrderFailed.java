package io.axoniq.demo.orderfulfillment.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record OrderFailed(
        String orderId,
        String reason
) {
}
