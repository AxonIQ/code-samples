package io.axoniq.demo.orderfulfillment.projection;

import io.axoniq.demo.orderfulfillment.api.InitiatingPaymentForCustomerStarted;
import io.axoniq.demo.orderfulfillment.api.OrderDelivered;
import io.axoniq.demo.orderfulfillment.api.OrderFailed;
import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.ShipOrderCompleted;
import io.axoniq.demo.orderfulfillment.api.TruckLocationUpdated;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes workflow Started/Completed updates plus simulator location ticks to subscribed SSE
 * clients. New subscribers receive a snapshot of all known orders so the map can render existing
 * shipments immediately.
 */
@Component
public class OrderEventStream {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventStream.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final OrderStatusProjection projection;

    public OrderEventStream(OrderStatusProjection projection) {
        this.projection = projection;
    }

    public SseEmitter subscribe() {
        var emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("snapshot").data(projection.findAll()));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @EventHandler
    public void on(OrderPlaced event) {
        var payload = new HashMap<String, Object>();
        payload.put("type", "PLACED");
        payload.put("orderId", event.orderId());
        payload.put("customerId", event.customerId());
        payload.put("email", event.email());
        payload.put("amount", event.amount());
        payload.put("originCity", event.originCity());
        payload.put("originLat", event.originLat());
        payload.put("originLng", event.originLng());
        payload.put("destinationCity", event.destinationCity());
        payload.put("destinationLat", event.destinationLat());
        payload.put("destinationLng", event.destinationLng());
        payload.put("scenario", event.scenario());
        payload.put("timestamp", Instant.now().toString());
        broadcast("order", payload);
    }

    @EventHandler
    public void on(InitiatingPaymentForCustomerStarted event) {
        broadcast("order", Map.of(
                "type", "AWAITING_PAYMENT",
                "orderId", event.orderId(),
                "timestamp", Instant.now().toString()
        ));
    }

    @EventHandler
    public void on(ShipOrderCompleted event) {
        broadcast("order", Map.of(
                "type", "IN_TRANSIT",
                "orderId", event.orderId(),
                "trackingNumber", event.trackingNumber(),
                "timestamp", Instant.now().toString()
        ));
    }

    @EventHandler
    public void on(TruckLocationUpdated event) {
        broadcast("location", Map.of(
                "orderId", event.orderId(),
                "lat", event.lat(),
                "lng", event.lng(),
                "progress", event.progress()
        ));
    }

    @EventHandler
    public void on(OrderDelivered event) {
        broadcast("order", Map.of(
                "type", "DELIVERED",
                "orderId", event.orderId(),
                "timestamp", Instant.now().toString()
        ));
    }

    @EventHandler
    public void on(OrderFailed event) {
        broadcast("order", Map.of(
                "type", "FAILED",
                "orderId", event.orderId(),
                "reason", event.reason(),
                "timestamp", Instant.now().toString()
        ));
    }

    private void broadcast(String name, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(name).data(payload));
            } catch (IOException | IllegalStateException e) {
                logger.debug("Dropping dead SSE emitter: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
