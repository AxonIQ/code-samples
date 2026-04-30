package io.axoniq.demo.orderfulfillment.projection;

import io.axoniq.demo.orderfulfillment.api.InitiatingPaymentForCustomerStarted;
import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.ShipOrderCompleted;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes workflow Started/Completed updates to subscribed SSE clients. Uses the same workflow
 * events the {@link OrderStatusProjection} listens to, so the live view reflects the durable
 * projection state.
 */
@Component
public class OrderEventStream {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventStream.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        var emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    @EventHandler
    public void on(OrderPlaced event) {
        broadcast(Map.of(
                "type", "PLACED",
                "orderId", event.orderId(),
                "customerId", event.customerId(),
                "email", event.email(),
                "amount", event.amount(),
                "timestamp", Instant.now().toString()
        ));
    }

    @EventHandler
    public void on(InitiatingPaymentForCustomerStarted event) {
        broadcast(Map.of(
                "type", "AWAITING_PAYMENT",
                "orderId", event.orderId(),
                "timestamp", Instant.now().toString()
        ));
    }

    @EventHandler
    public void on(ShipOrderCompleted event) {
        broadcast(Map.of(
                "type", "SHIPPED",
                "orderId", event.orderId(),
                "trackingNumber", event.trackingNumber(),
                "timestamp", Instant.now().toString()
        ));
    }

    private void broadcast(Map<String, Object> payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("order").data(payload));
            } catch (IOException | IllegalStateException e) {
                logger.debug("Dropping dead SSE emitter: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
