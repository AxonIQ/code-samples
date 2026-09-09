package io.axoniq.demo.workflowsaga.workflow;

import io.axoniq.demo.workflowsaga.api.OrderConfirmedEvent;
import io.axoniq.demo.workflowsaga.api.OrderPaidEvent;
import io.axoniq.demo.workflowsaga.api.OrderPaymentCancelledEvent;
import io.axoniq.demo.workflowsaga.api.ShipmentStatus;
import io.axoniq.demo.workflowsaga.api.ShipmentStatusUpdatedEvent;
import io.axoniq.workflow.dsl.simple.SimpleWorkflowContext;
import io.axoniq.workflow.runtime.api.annotation.Workflow;
import io.axoniq.workflow.runtime.api.execution.context.EventNameCustomizer;
import io.axoniq.workflow.runtime.api.execution.state.WorkflowStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.axoniq.workflow.dsl.api.EventAssociationsUtils.equalsTo;
import static io.axoniq.workflow.dsl.api.EventAssociationsUtils.payloadProperty;
import static io.axoniq.workflow.dsl.api.Payload.payload;
import static io.axoniq.workflow.runtime.association.Associations.associate;
import static io.axoniq.workflow.runtime.execution.DefaultEventNameCustomizer.Builder.namespace;

/**
 * Workflow rewrite of the legacy {@code ProcessOrderSaga}.
 * <p>
 * The workflow does not call {@code eventGateway.publish(...)} anywhere — every step the workflow
 * runs (the {@code awaitExecute} calls) automatically produces a Started + Completed event in the
 * event store. Those emitted events are the durable signal external systems and projections
 * subscribe to.
 */
@Component
public class ProcessOrderWorkflow {

    private static final Logger logger = LoggerFactory.getLogger(ProcessOrderWorkflow.class);
    private static final Duration ORDER_DEADLINE = Duration.ofDays(5);
    private static final Duration STEP_TIMEOUT = Duration.ofSeconds(30);

    /** Emit step events under our own namespace so the projection's @{@code @EventHandler} records line up by FQCN. */
    private static EventNameCustomizer apiNamespace() {
        return namespace("io.axoniq.demo.workflowsaga.api");
    }

    @Workflow(
            idProperty = "orderId",
            startOnEventClass = OrderConfirmedEvent.class,
            workflowName = "ProcessOrderWorkflow"
    )
    public void execute(SimpleWorkflowContext ctx) {
        var orderId = (String) ctx.workflowPayload().get("orderId");
        var paymentId = UUID.randomUUID().toString();
        var shipmentId = UUID.randomUUID().toString();
        ctx.setPayload("registerIds", payload("paymentId", paymentId, "shipmentId", shipmentId));

        logger.info("Order {} workflow started (payment {}, shipment {}).", orderId, paymentId, shipmentId);

        var paid = ctx.waitForEvent(
                "paid",
                OrderPaidEvent.class,
                associate(payloadProperty("paymentId"), equalsTo(paymentId)),
                step -> step.timeout(ORDER_DEADLINE)
        );
        var paymentCancelled = ctx.waitForEvent(
                "paymentCancelled",
                OrderPaymentCancelledEvent.class,
                associate(payloadProperty("paymentId"), equalsTo(paymentId)),
                step -> step.timeout(ORDER_DEADLINE)
        );
        var delivered = ctx.waitForEvent(
                "delivered",
                ShipmentStatusUpdatedEvent.class,
                associate(payloadProperty("shipmentId"), equalsTo(shipmentId))
                        .and(payloadProperty("shipmentStatus"), "=", ShipmentStatus.DELIVERED.name()),
                step -> step.timeout(ORDER_DEADLINE)
        );

        // The Started events of these steps — `RequestPaymentStarted` and `RequestShipmentStarted`
        // — are the durable signals projections subscribe to. No manual publishing needed.
        ctx.awaitExecute(
                "requestPayment",
                Map.of("orderId", orderId, "paymentId", paymentId),
                (pc, p) -> Map.of(),
                def -> def.timeout(STEP_TIMEOUT).eventNameCustomizer(apiNamespace())
        );
        ctx.awaitExecute(
                "requestShipment",
                Map.of("orderId", orderId, "shipmentId", shipmentId),
                (pc, p) -> Map.of(),
                def -> def.timeout(STEP_TIMEOUT).eventNameCustomizer(apiNamespace())
        );

        var paymentOutcome = ctx.anyMatch(WorkflowStepResult::success, paid, paymentCancelled);
        paymentOutcome.await();

        var matched = paymentOutcome.matched().stream().map(WorkflowStepResult::getStepName).toList();
        if (matched.contains("paymentCancelled")) {
            logger.info("Order {} payment cancelled — cancelling shipment.", orderId);
            ctx.awaitExecute(
                    "cancelShipment",
                    Map.of("orderId", orderId, "shipmentId", shipmentId),
                    (pc, p) -> Map.of(),
                    def -> def.timeout(STEP_TIMEOUT).eventNameCustomizer(apiNamespace())
            );
            delivered.cancel("payment cancelled");
            ctx.awaitExecute(
                    "completeOrder",
                    Map.of("orderId", orderId, "paid", false, "delivered", false),
                    (pc, p) -> Map.of(),
                    def -> def.timeout(STEP_TIMEOUT).eventNameCustomizer(apiNamespace())
            );
            return;
        }

        delivered.await();
        var isDelivered = delivered.success();
        logger.info("Order {} completed (paid {}, delivered {}).", orderId, true, isDelivered);
        ctx.awaitExecute(
                "completeOrder",
                Map.of("orderId", orderId, "paid", true, "delivered", isDelivered),
                (pc, p) -> Map.of(),
                def -> def.timeout(STEP_TIMEOUT).eventNameCustomizer(apiNamespace())
        );
    }
}
