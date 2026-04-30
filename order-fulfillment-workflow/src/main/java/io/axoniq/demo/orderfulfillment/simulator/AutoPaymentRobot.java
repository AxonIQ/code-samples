package io.axoniq.demo.orderfulfillment.simulator;

import io.axoniq.demo.orderfulfillment.api.InitiatingPaymentForCustomerStarted;
import io.axoniq.demo.orderfulfillment.api.PaymentConfirmed;
import io.axoniq.demo.orderfulfillment.projection.OrderStatus;
import io.axoniq.demo.orderfulfillment.projection.OrderStatusProjection;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.gateway.EventGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Auto-confirms payments after a randomized delay so the demo runs end-to-end without manual
 * clicks. Skips orders flagged with the {@code payment-timeout} scenario, letting the workflow's
 * waitForEvent expire on purpose.
 */
@Component
public class AutoPaymentRobot {

    private static final Logger logger = LoggerFactory.getLogger(AutoPaymentRobot.class);

    private final EventGateway eventGateway;
    private final OrderStatusProjection projection;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                var t = new Thread(r, "auto-payment");
                t.setDaemon(true);
                return t;
            });

    @Autowired
    public AutoPaymentRobot(EventGateway eventGateway, OrderStatusProjection projection) {
        this.eventGateway = eventGateway;
        this.projection = projection;
    }

    @EventHandler
    public void on(InitiatingPaymentForCustomerStarted event) {
        var orderId = event.orderId();
        var status = projection.findById(orderId).orElse(null);
        var scenario = status == null ? null : status.scenario();
        if ("payment-timeout".equals(scenario)) {
            logger.info("Skipping auto-payment for {} — scenario forces timeout.", orderId);
            return;
        }
        var delayMs = ThreadLocalRandom.current().nextInt(800, 3500);
        scheduler.schedule(() -> confirm(orderId), delayMs, TimeUnit.MILLISECONDS);
    }

    private void confirm(String orderId) {
        var status = projection.findById(orderId).orElse(null);
        if (status == null || status.status() == OrderStatus.Status.FAILED) {
            return;
        }
        var transactionId = "txn-" + UUID.randomUUID();
        logger.info("Auto-confirming payment for {} ({}).", orderId, transactionId);
        eventGateway.publish(null, new PaymentConfirmed(orderId, transactionId));
    }
}
