package io.axoniq.demo.orderfulfillment.simulator;

import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import org.axonframework.messaging.eventhandling.gateway.EventGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class Simulator {

    private static final Logger logger = LoggerFactory.getLogger(Simulator.class);

    private final EventGateway eventGateway;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                var t = new Thread(r, "sim-burst");
                t.setDaemon(true);
                return t;
            });

    public Simulator(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    public String placeRandom(String scenario) {
        var customer = CustomerPool.random();
        var route = Cities.randomPair();
        var orderId = UUID.randomUUID().toString();
        var event = new OrderPlaced(
                orderId,
                customer.id(),
                customer.email(),
                CustomerPool.randomAmount(),
                route[0].name(), route[0].lat(), route[0].lng(),
                route[1].name(), route[1].lat(), route[1].lng(),
                scenario
        );
        logger.info("Placing simulated order {} ({} → {}, scenario={}).",
                    orderId, route[0].name(), route[1].name(), scenario);
        eventGateway.publish(null, event);
        return orderId;
    }

    public void burst(int count, String scenario) {
        var n = Math.max(1, Math.min(count, 100));
        for (int i = 0; i < n; i++) {
            var staggerMs = ThreadLocalRandom.current().nextInt(50, 350);
            scheduler.schedule(() -> placeRandom(scenario), (long) i * staggerMs, TimeUnit.MILLISECONDS);
        }
    }
}
