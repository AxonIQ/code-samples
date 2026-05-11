package io.axoniq.demo.workflowsaga.projection;

import io.axoniq.demo.workflowsaga.api.RequestPaymentStarted;
import io.axoniq.demo.workflowsaga.api.RequestShipmentStarted;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures the {@code paymentId}/{@code shipmentId} that the workflow generates per order, so the
 * integration test (or a UI) can correlate domain events back to the original order. The
 * subscribed events are emitted automatically by the workflow's {@code awaitExecute} steps —
 * nothing publishes them by hand.
 */
@Component
public class IdRegistry {

    public record Ids(String paymentId, String shipmentId) {
    }

    private final Map<String, Ids> byOrder = new ConcurrentHashMap<>();

    @EventHandler
    public void on(RequestPaymentStarted event) {
        byOrder.compute(event.orderId(), (id, current) -> current == null
                ? new Ids(event.paymentId(), null)
                : new Ids(event.paymentId(), current.shipmentId()));
    }

    @EventHandler
    public void on(RequestShipmentStarted event) {
        byOrder.compute(event.orderId(), (id, current) -> current == null
                ? new Ids(null, event.shipmentId())
                : new Ids(current.paymentId(), event.shipmentId()));
    }

    public Optional<Ids> forOrder(String orderId) {
        return Optional.ofNullable(byOrder.get(orderId));
    }
}
