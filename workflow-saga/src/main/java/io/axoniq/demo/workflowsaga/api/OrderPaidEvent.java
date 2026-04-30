package io.axoniq.demo.workflowsaga.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record OrderPaidEvent(String paymentId) {
}
