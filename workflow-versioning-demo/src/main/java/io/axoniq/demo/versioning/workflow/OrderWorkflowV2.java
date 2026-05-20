package io.axoniq.demo.versioning.workflow;

import io.axoniq.demo.versioning.api.PaymentReceivedEvent;
import io.axoniq.demo.versioning.service.DemoServices;
import io.axoniq.workflow.dsl.simple.SimpleWorkflowContext;
import io.axoniq.workflow.runtime.api.annotation.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static io.axoniq.workflow.dsl.api.AssociationsUtils.associate;
import static io.axoniq.workflow.dsl.simple.SimpleWorkflowContext.equalsTo;
import static io.axoniq.workflow.runtime.association.PayloadPropertyValueRetriever.payloadProperty;

/**
 * Order workflow v1.0.0 — a copy-paste of {@link OrderWorkflow} with a bumped annotation version and a
 * structurally different body (premium customers get expedited shipping; non-premium customers go through
 * a fraud-review step).
 * <p>
 * Demonstrates the "copy the whole class and bump the version" pattern: with both this class and
 * {@link OrderWorkflow} registered, the runtime spawns new instances on this (higher) version while
 * replaying any in-flight v0.0.1 instances on the legacy {@link OrderWorkflow} definition — selection is
 * driven by the started event's {@code MessageType.version()} captured into state.
 */
@Component
public class OrderWorkflowV2 {

    private static final Logger logger = LoggerFactory.getLogger(OrderWorkflowV2.class);

    private final DemoServices services;

    public OrderWorkflowV2(DemoServices services) {
        this.services = services;
    }

    @Workflow(
            idProperty = "orderId",
            startOnEventName = "io.axoniq.demo.versioning.OrderPlaced",
            workflowName = "OrderWorkflow",
            version = "2.0.0"
    )
    public void execute(SimpleWorkflowContext ctx) {
        var orderId = (String) ctx.workflowPayload().get("orderId");
        var customerId = (String) ctx.workflowPayload().get("customerId");
        var amount = ((Number) ctx.workflowPayload().get("amount")).intValue();

        String v = ctx.version("address-validation", "2.0.1");

        if ("2.0.1".equals(v)) {
            ctx.awaitExecute("validateAddress", Boolean.class,
                             () -> services.validateAddress(orderId));
        }

        logger.info("Order {} V2 workflow started for customer {} (running version {})",
                    orderId, customerId, ctx.workflowVersion());

        ctx.awaitExecute("reserveStock", Boolean.class, () -> services.reserveStock(orderId));

        // v1.0.0-only: explicit address validation always runs (no longer behind a ctx.version flag).
        ctx.awaitExecute("validateAddress", Boolean.class,
                         () -> services.validateAddress(orderId));

        // v2.0.0-only: branch on a payload attribute. This body is intentionally structurally different
        // from OrderWorkflow's so it's clear when a workflow instance is replaying the V2 definition.
        var premium = Boolean.TRUE.equals(ctx.workflowPayload().get("premium"));
        if (!premium) {
            ctx.awaitExecute("fraudReview", Boolean.class,
                             () -> services.fraudReview(orderId));
        }

        ctx.awaitExecute("chargePayment", Boolean.class,
                         () -> services.chargePayment(orderId, amount));

        var payment = ctx.awaitWaitFor("awaitPayment",
                                       PaymentReceivedEvent.class,
                                       associate(payloadProperty("orderId"), equalsTo(orderId)),
                                       step -> step.timeout(Duration.ofMinutes(5))
        );
        logger.info("Order {} V2 received payment for ${}", orderId, payment.amount());

        ctx.awaitExecute("notifyCustomer", Boolean.class, () -> {
            services.notifyCustomer(orderId, customerId);
            return true;
        });

        logger.info("Order {} V2 workflow completed at version {}", orderId, ctx.workflowVersion());
    }
}
