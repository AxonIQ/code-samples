package io.axoniq.demo.orderfulfillment.workflow;

import io.axoniq.demo.orderfulfillment.api.PaymentConfirmed;
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
import java.util.Map;

import static io.axoniq.workflow.dsl.api.AssociationsUtils.associate;
import static io.axoniq.workflow.dsl.simple.SimpleWorkflowContext.equalsTo;
import static io.axoniq.workflow.runtime.association.PayloadPropertyValueRetriever.payloadProperty;
import static io.axoniq.workflow.runtime.execution.DefaultEventNameCustomizer.Builder.baseName;
import static io.axoniq.workflow.runtime.execution.DefaultEventNameCustomizer.Builder.namespace;

@Component
public class OrderFulfillmentWorkflow {

    private static final Logger logger = LoggerFactory.getLogger(OrderFulfillmentWorkflow.class);

    private final InventoryService inventory;
    private final PaymentService payment;
    private final ShippingService shipping;
    private final NotificationService notifications;

    public OrderFulfillmentWorkflow(InventoryService inventory,
                                    PaymentService payment,
                                    ShippingService shipping,
                                    NotificationService notifications) {
        this.inventory = inventory;
        this.payment = payment;
        this.shipping = shipping;
        this.notifications = notifications;
    }

    @Workflow(
            idProperty = "orderId",
            startOnEvent = "io.axoniq.demo.orderfulfillment.api.OrderPlaced",
            workflowName = "OrderFulfillmentWorkflow"
    )
    public void execute(SimpleWorkflowContext ctx) {
        var orderId = (String) ctx.workflowPayload().get("orderId");
        var customerId = (String) ctx.workflowPayload().get("customerId");
        var email = (String) ctx.workflowPayload().get("email");
        var amount = ctx.workflowPayload().get("amount");

        logger.info("Order {} workflow started for customer {} (amount {}).", orderId, customerId, amount);

        var paymentConfirmation = ctx.waitForEvent(
                "awaitPayment",
                PaymentConfirmed.class,
                associate(payloadProperty("orderId"), equalsTo(orderId)),
                Duration.ofMinutes(15)
        );

        var reserved = ctx.awaitExecute(
                "reserveStock",
                Map.of("customerId", customerId, "amount", amount),
                Boolean.class,
                inventory::reserveStock
        );
        if (!reserved) {
            paymentConfirmation.cancel("Stock unavailable");
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
                Duration.ofSeconds(30),
                baseName("InitiatingPaymentForCustomer").namespace("io.axoniq.demo.orderfulfillment.api")
        );

        paymentConfirmation.await();
        var confirmation = paymentConfirmation.<Map<String, Object>>result()
                                              .orElseThrow(() -> new IllegalStateException("Payment not confirmed"));
        var transactionId = (String) confirmation.get("transactionId");
        logger.info("Order {} payment confirmed (transaction {}).", orderId, transactionId);

        // The Completed event of this step is `ShipOrderCompleted` (in our api namespace) — that is
        // the durable "order shipped" signal projections subscribe to. Nothing publishes it manually.
        var shipResult = ctx.awaitExecute(
                "shipOrder",
                Map.of("orderId", orderId, "transactionId", transactionId),
                (pc, p) -> shipping.shipOrder(p),
                Duration.ofSeconds(30),
                namespace("io.axoniq.demo.orderfulfillment.api")
        );
        var trackingNumber = (String) shipResult.get("trackingNumber");

        ctx.awaitExecute("notifyCustomer", Boolean.class, () -> {
            notifications.sendConfirmation(email, trackingNumber);
            return true;
        });

        logger.info("Order {} workflow completed (tracking {}).", orderId, trackingNumber);
    }
}
