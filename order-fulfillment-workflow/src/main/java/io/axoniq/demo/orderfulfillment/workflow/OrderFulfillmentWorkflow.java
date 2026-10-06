package io.axoniq.demo.orderfulfillment.workflow;

import io.axoniq.demo.orderfulfillment.api.OrderFailed;
import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.PaymentConfirmed;
import io.axoniq.demo.orderfulfillment.service.InventoryService;
import io.axoniq.demo.orderfulfillment.service.NotificationService;
import io.axoniq.demo.orderfulfillment.service.PaymentService;
import io.axoniq.demo.orderfulfillment.service.ShippingService;
import io.axoniq.framework.workflow.annotation.Workflow;
import io.axoniq.framework.workflow.dsl.api.CombinatorWorkflowStepResult;
import io.axoniq.framework.workflow.dsl.api.ExecuteStepDefinition;
import io.axoniq.framework.workflow.dsl.api.WorkflowStepResult;
import io.axoniq.framework.workflow.dsl.simple.SimpleWorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

import static io.axoniq.framework.workflow.dsl.api.EventAssociationsUtils.equalsTo;
import static io.axoniq.framework.workflow.dsl.api.EventAssociationsUtils.payloadProperty;
import static io.axoniq.framework.workflow.runtime.association.Associations.associate;
import static io.axoniq.framework.workflow.runtime.execution.DefaultEventNameCustomizer.Builder.baseName;

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
            startOnEventClass = OrderPlaced.class,
            workflowName = "OrderFulfillmentWorkflow"
    )
    public void execute(SimpleWorkflowContext ctx) {
        var paymentConfirmation = registerPaymentWait(ctx);

        if (!reserveStock(ctx)) {
            rejectOutOfStock(ctx, paymentConfirmation);
            return;
        }

        awaitPayment(ctx, paymentConfirmation);
        var trackingNumber = shipOrder(ctx);
        notifyCustomer(ctx, trackingNumber);
    }

    private WorkflowStepResult registerPaymentWait(SimpleWorkflowContext ctx) {
        var order = ctx.workflowPayload();
        logger.info("Order {} workflow started for customer {} (amount {}, scenario {}).",
                    order.get("orderId"), order.get("customerId"), order.get("amount"), order.get("scenario"));

        var paymentTimeout = "payment-timeout".equals(order.get("scenario"))
                ? Duration.ofSeconds(4)
                : Duration.ofSeconds(45);

        // Register before any action that could trigger a payment response.
        return ctx.waitForEvent(
                "awaitPayment",
                PaymentConfirmed.class,
                associate(payloadProperty("orderId"), equalsTo(order.get("orderId"))),
                step -> step.timeout(paymentTimeout)
        );
    }

    private boolean reserveStock(SimpleWorkflowContext ctx) {
        var order = ctx.workflowPayload();
        var reservation = ctx.awaitExecute(
                "reserveStock",
                Map.of("customerId", order.get("customerId"),
                       "amount", order.get("amount"), "scenario", order.get("scenario")),
                (pc, input) -> Map.of("reserved", inventory.reserveStock(input))
        );
        return (boolean) reservation.get("reserved");
    }

    private void awaitPayment(SimpleWorkflowContext ctx, WorkflowStepResult paymentConfirmation) {
        var paymentInitiation = initiatePayment(ctx);
        var payment = ctx.allMatch(WorkflowStepResult::success, paymentConfirmation, paymentInitiation);
        payment.await();
        if (!payment.success()) {
            rejectPayment(ctx, payment);
        }
    }

    private WorkflowStepResult initiatePayment(SimpleWorkflowContext ctx) {
        var order = ctx.workflowPayload();
        return ctx.execute(
                "initiatePayment",
                Map.of("orderId", order.get("orderId"), "customerId", order.get("customerId"),
                       "amount", order.get("amount"), "scenario", order.get("scenario")),
                (pc, p) -> {
                    payment.initiatePayment(p);
                    return Map.of();
                },
                step -> serviceStep(step, "InitiatingPaymentForCustomer")
        );
    }

    private void rejectPayment(SimpleWorkflowContext ctx, CombinatorWorkflowStepResult payment) {
        var orderId = (String) ctx.workflowPayload().get("orderId");
        var unsuccessful = payment.unmatched().getFirst();
        var reason = paymentFailureReason(unsuccessful);
        payment.unmatched().stream()
               .filter(step -> !step.isCompleted())
               .forEach(step -> step.cancel(reason));

        var failureStep = "awaitPayment".equals(unsuccessful.getStepName()) && unsuccessful.timeout()
                ? "recordPaymentTimeout" : "recordPaymentFailure";
        ctx.awaitPublish(failureStep, new OrderFailed(orderId, reason));
        ctx.fail(new RuntimeException(reason + " for order " + orderId));
    }

    private String paymentFailureReason(WorkflowStepResult step) {
        if ("initiatePayment".equals(step.getStepName())) {
            if (step.timeout()) {
                return "Payment initiation timed out";
            }
            return step.canceled() ? "Payment initiation cancelled" : "Payment initiation failed";
        }
        if (step.timeout()) {
            return "Payment timed out";
        }
        return step.canceled() ? "Payment wait cancelled" : "Payment confirmation failed";
    }

    private String shipOrder(SimpleWorkflowContext ctx) {
        var order = ctx.workflowPayload();
        // Create the tracking number inside the action so replay uses its recorded result.
        var shipment = ctx.awaitExecute(
                "shipOrder",
                Map.of("orderId", order.get("orderId"),
                       "originLat", ((Number) order.get("originLat")).doubleValue(),
                       "originLng", ((Number) order.get("originLng")).doubleValue(),
                       "destinationLat", ((Number) order.get("destinationLat")).doubleValue(),
                       "destinationLng", ((Number) order.get("destinationLng")).doubleValue()),
                (pc, p) -> shipping.shipOrder(p),
                step -> serviceStep(step, "ShipOrder")
        );
        return (String) shipment.get("trackingNumber");
    }

    private void notifyCustomer(SimpleWorkflowContext ctx, String trackingNumber) {
        var email = (String) ctx.workflowPayload().get("email");
        ctx.awaitExecute("notifyCustomer", Boolean.class, () -> {
            notifications.sendConfirmation(email, trackingNumber);
            return true;
        });
        logger.info("Order {} workflow completed (tracking {}).", ctx.workflowPayload().get("orderId"), trackingNumber);
    }

    private void rejectOutOfStock(SimpleWorkflowContext ctx, WorkflowStepResult paymentConfirmation) {
        var orderId = (String) ctx.workflowPayload().get("orderId");
        paymentConfirmation.cancel("Stock unavailable");
        ctx.awaitPublish("recordOutOfStock", new OrderFailed(orderId, "Out of stock"));
        ctx.fail(new RuntimeException("Stock unavailable for order " + orderId));
    }

    private static ExecuteStepDefinition serviceStep(ExecuteStepDefinition step, String eventBaseName) {
        return step.timeout(Duration.ofSeconds(30))
                   .eventNameCustomizer(baseName(eventBaseName).namespace(OrderPlaced.class.getPackageName()));
    }
}
