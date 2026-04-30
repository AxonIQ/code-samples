package io.axoniq.demo.orderfulfillment.simulator;

import io.axoniq.demo.orderfulfillment.api.OrderDelivered;
import io.axoniq.demo.orderfulfillment.api.ShipOrderCompleted;
import io.axoniq.demo.orderfulfillment.api.TruckLocationUpdated;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.gateway.EventGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Animates truck movement after the workflow's {@code shipOrder} step completes. Publishes
 * {@link TruckLocationUpdated} ticks along the great-circle-ish straight line between origin and
 * destination, then emits a final {@link OrderDelivered} event. Decoupled from the workflow so
 * many shipments can animate concurrently without blocking workflow threads.
 */
@Component
public class TruckMovementSimulator {

    private static final Logger logger = LoggerFactory.getLogger(TruckMovementSimulator.class);
    private static final int TICKS = 30;
    private static final long TICK_INTERVAL_MS = 400;

    private final EventGateway eventGateway;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(8, r -> {
                var t = new Thread(r, "truck-sim");
                t.setDaemon(true);
                return t;
            });

    public TruckMovementSimulator(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    @EventHandler
    public void on(ShipOrderCompleted event) {
        var jitter = ThreadLocalRandom.current().nextDouble(0.85, 1.25);
        var orderId = event.orderId();
        var trackingNumber = event.trackingNumber();
        var originLat = event.originLat();
        var originLng = event.originLng();
        var destLat = event.destinationLat();
        var destLng = event.destinationLng();

        logger.info("Starting truck animation for order {} ({}).", orderId, trackingNumber);

        var step = new AtomicInteger(1);
        var handle = scheduler.scheduleAtFixedRate(() -> {
            int i = step.getAndIncrement();
            if (i > TICKS) {
                return;
            }
            double t = (double) i / TICKS;
            double lat = originLat + (destLat - originLat) * t;
            double lng = originLng + (destLng - originLng) * t;
            try {
                eventGateway.publish(null, new TruckLocationUpdated(orderId, lat, lng, t));
            } catch (Exception e) {
                logger.warn("Failed to publish location for {}: {}", orderId, e.getMessage());
            }
        }, 100, (long) (TICK_INTERVAL_MS * jitter), TimeUnit.MILLISECONDS);

        scheduler.schedule(() -> {
            handle.cancel(false);
            try {
                eventGateway.publish(null, new OrderDelivered(orderId, trackingNumber));
                logger.info("Order {} delivered.", orderId);
            } catch (Exception e) {
                logger.warn("Failed to publish delivery for {}: {}", orderId, e.getMessage());
            }
        }, (long) (TICK_INTERVAL_MS * jitter * (TICKS + 1)), TimeUnit.MILLISECONDS);
    }
}
