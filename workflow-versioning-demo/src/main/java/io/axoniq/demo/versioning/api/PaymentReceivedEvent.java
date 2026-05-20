package io.axoniq.demo.versioning.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event(namespace = "io.axoniq.demo.versioning", name = "PaymentReceived")
public record PaymentReceivedEvent(String orderId, int amount) {
}
