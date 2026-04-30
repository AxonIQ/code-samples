package io.axoniq.demo.orderfulfillment.service;

import io.axoniq.demo.orderfulfillment.api.OrderFailed;
import org.axonframework.messaging.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Lets the workflow publish an {@link OrderFailed} event from inside an {@code awaitExecute}
 * step, so failure broadcasts go through the regular event pipeline instead of being side-effects
 * of the workflow body.
 */
@Component
public class FailureRecorder {

    private final EventGateway eventGateway;

    public FailureRecorder(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    public Boolean record(Map<String, Object> payload) {
        var orderId = (String) payload.get("orderId");
        var reason = (String) payload.get("reason");
        eventGateway.publish(null, new OrderFailed(orderId, reason));
        return true;
    }
}
