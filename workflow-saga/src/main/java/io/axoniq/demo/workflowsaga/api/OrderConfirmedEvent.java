package io.axoniq.demo.workflowsaga.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record OrderConfirmedEvent(String orderId) {
}
