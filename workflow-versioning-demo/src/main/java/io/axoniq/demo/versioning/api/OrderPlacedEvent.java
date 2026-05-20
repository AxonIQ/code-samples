package io.axoniq.demo.versioning.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event(namespace = "io.axoniq.demo.versioning", name = "OrderPlaced")
public record OrderPlacedEvent(String orderId, String customerId, int amount) {
}
