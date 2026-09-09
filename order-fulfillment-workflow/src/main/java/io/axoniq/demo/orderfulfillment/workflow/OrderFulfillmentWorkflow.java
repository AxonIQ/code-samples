package io.axoniq.demo.orderfulfillment.workflow;

import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.PaymentConfirmed;
import io.axoniq.demo.orderfulfillment.service.FailureRecorder;
import io.axoniq.demo.orderfulfillment.service.InventoryService;
import io.axoniq.demo.orderfulfillment.service.NotificationService;
import io.axoniq.demo.orderfulfillment.service.PaymentService;
import io.axoniq.demo.orderfulfillment.service.ShippingService;
import io.axoniq.workflow.dsl.simple.SimpleWorkflowContext;
import io.axoniq.workflow.runtime.api.annotation.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.axoniq.workflow.dsl.api.EventAssociationsUtils.equalsTo;
import static io.axoniq.workflow.dsl.api.EventAssociationsUtils.payloadProperty;
import static io.axoniq.workflow.runtime.association.Associations.associate;
import static io.axoniq.workflow.runtime.execution.DefaultEventNameCustomizer.Builder.baseName;
import static io.axoniq.workflow.runtime.execution.DefaultEventNameCustomizer.Builder.namespace;

@Component
public class OrderFulfillmentWorkflow {

    private static final Logger logger = LoggerFactory.getLogger(OrderFulfillmentWorkflow.class);

    private final InventoryService inventory;
    private final PaymentService payment;
    private final ShippingService shipping;
    private final NotificationService notifications;
    private final FailureRecorder failures;

    public OrderFulfillmentWorkflow(InventoryService inventory,
                                    PaymentService payment,
                                    ShippingService shipping,
                                    NotificationService notifications,
                                    FailureRecorder failures) {
        this.inventory = inventory;
        this.payment = payment;
        this.shipping = shipping;
        this.notifications = notifications;
        this.failures = failures;
    }

    @Workflow(
            idProperty = "orderId",
            startOnEventClass = OrderPlaced.class,
            workflowName = "OrderFulfillmentWorkflow"
    )
    public void execute(SimpleWorkflowContext ctx) {
        var orderId = (String) ctx.workflowPayload().get("orderId");
        var customerId = (String) ctx.workflowPayload().get("customerId");
        var email = (String) ctx.workflowPayload().get("email");
        var amount = ctx.workflowPayload().get("amount");
        var scenario = (String) ctx.workflowPayload().get("scenario");
        var originLat = ((Number) ctx.workflowPayload().get("originLat")).doubleValue();
        var originLng = ((Number) ctx.workflowPayload().get("originLng")).doubleValue();
        var destLat = ((Number) ctx.workflowPayload().get("destinationLat")).doubleValue();
        var destLng = ((Number) ctx.workflowPayload().get("destinationLng")).doubleValue();

        logger.info("Order {} workflow started for customer {} (amount {}, scenario {}).",
                    orderId, customerId, amount, scenario);

        var paymentTimeout = "payment-timeout".equals(scenario)
                ? Duration.ofSeconds(4)
                : Duration.ofSeconds(45);

        var paymentConfirmation = ctx.waitForEvent(
                "awaitPayment",
                PaymentConfirmed.class,
                associate(payloadProperty("orderId"), equalsTo(orderId)),
                step -> step.timeout(paymentTimeout)
        );

        var reserved = (Boolean) ctx.awaitExecute(
                "reserveStock",
                Map.of("customerId", customerId, "amount", amount, "scenario", scenario),
                (pc, p) -> Map.of("result", inventory.reserveStock(p))
        ).get("result");
        if (!reserved) {
            paymentConfirmation.cancel("Stock unavailable");
            ctx.awaitExecute(
                    "recordOutOfStock",
                    Map.of("orderId", orderId, "reason", "Out of stock"),
                    (pc, p) -> Map.of("result", failures.record(p))
            );
            ctx.fail(new RuntimeException("Stock unavailable for order " + orderId));
            return;
        }

        ctx.awaitExecute(
                "initiatePayment",
                Map.of("orderId", orderId, "customerId", customerId, "amount", amount),
                (pc, p) -> {
                    payment.initiatePayment(p);
                    return Map.of();
                },
                def -> def.timeout(Duration.ofSeconds(30))
                        .eventNameCustomizer(baseName("InitiatingPaymentForCustomer")
                                                     .namespace("io.axoniq.demo.orderfulfillment.api"))
        );

        paymentConfirmation.await();
        var confirmation = paymentConfirmation.result();
        if (confirmation.isEmpty()) {
            ctx.awaitExecute(
                    "recordPaymentTimeout",
                    Map.of("orderId", orderId, "reason", "Payment timed out"),
                    (pc, p) -> Map.of("result", failures.record(p))
            );
            ctx.fail(new RuntimeException("Payment timed out for order " + orderId));
            return;
        }
        var transactionId = (String) confirmation.get().get("transactionId");
        logger.info("Order {} payment confirmed (transaction {}).", orderId, transactionId);

        // The Completed event of this step is `ShipOrderCompleted` — that is the durable signal
        // the truck-movement simulator subscribes to. The trackingNumber is generated up front so
        // both the Started and Completed events expose it.
        var trackingNumber = "TRK-" + UUID.randomUUID();
        var shipPayload = new HashMap<String, Object>();
        shipPayload.put("orderId", orderId);
        shipPayload.put("trackingNumber", trackingNumber);
        shipPayload.put("originLat", originLat);
        shipPayload.put("originLng", originLng);
        shipPayload.put("destinationLat", destLat);
        shipPayload.put("destinationLng", destLng);
        ctx.awaitExecute(
                "shipOrder",
                shipPayload,
                (pc, p) -> shipping.shipOrder(p),
                def -> def.timeout(Duration.ofSeconds(30))
                        .eventNameCustomizer(namespace("io.axoniq.demo.orderfulfillment.api"))
        );

        ctx.awaitExecute("notifyCustomer", Boolean.class, () -> {
            notifications.sendConfirmation(email, trackingNumber);
            return true;
        });

        logger.info("Order {} workflow completed (tracking {}).", orderId, trackingNumber);
    }
}
