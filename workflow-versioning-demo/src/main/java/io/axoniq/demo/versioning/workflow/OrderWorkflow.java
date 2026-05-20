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
 * Order workflow v0.0.1: reserveStock → chargePayment → awaitPayment → notifyCustomer.
 * <p>
 * Demonstrates two versioning mechanisms:
 * <ul>
 *   <li><b>{@code @Workflow(version="0.0.1")}</b> — declares this definition's version. Every event the
 *       workflow emits is stamped with this version via AF5's native {@code MessageType.version()}.
 *       For breaking-change rewrites, copy this class to {@code OrderWorkflowV2} with {@code version="0.0.2"};
 *       the runtime spawns new instances on the highest version and replays in-flight instances on the
 *       version they were started under.</li>
 *   <li><b>{@code ctx.version(changeId, newVersion)}</b> — opt-in mid-run version bump for additive
 *       changes. Replace the {@code address-validation} block below with a {@code ctx.version(...)} call
 *       to roll out a new step safely against in-flight instances (the downstream-steps guard keeps
 *       workflows that already executed past this point on the legacy branch).</li>
 * </ul>
 */
@Component
public class OrderWorkflow {

    private static final Logger logger = LoggerFactory.getLogger(OrderWorkflow.class);

    private final DemoServices services;

    public OrderWorkflow(DemoServices services) {
        this.services = services;
    }

    @Workflow(
            idProperty = "orderId",
            startOnEventName = "io.axoniq.demo.versioning.OrderPlaced",
            workflowName = "OrderWorkflow",
            version = "1.0.0"
    )
    public void execute(SimpleWorkflowContext ctx) {
        var orderId = (String) ctx.workflowPayload().get("orderId");
        var customerId = (String) ctx.workflowPayload().get("customerId");
        var amount = ((Number) ctx.workflowPayload().get("amount")).intValue();

        logger.info("Order {} workflow started for customer {} (running version {})",
                    orderId, customerId, ctx.workflowVersion());

        ctx.awaitExecute("reserveStock", Boolean.class, () -> services.reserveStock(orderId));

        // Demo of the additive-change primitive. Bumping the workflow's version mid-flight via
        // ctx.version causes the address-validation step to run only for newly-started instances
        // (or instances paused before reaching this call). In-flight instances that already
        // executed past this position observe the current version unchanged and skip the new step.
//        String v = ctx.version("address-validation", "0.0.2");
//        if (!"0.0.1".equals(v)) {
//            ctx.awaitExecute("validateAddress", Boolean.class,
//                             () -> services.validateAddress(orderId));
//        }

        ctx.awaitExecute("chargePayment", Boolean.class,
                         () -> services.chargePayment(orderId, amount));

        var payment = ctx.awaitWaitFor("awaitPayment",
                                       PaymentReceivedEvent.class,
                                       associate(payloadProperty("orderId"), equalsTo(orderId)),
                                       step -> step.timeout(Duration.ofMinutes(15))
        );
        logger.info("Order {} received payment for ${}", orderId, payment.amount());

        ctx.awaitExecute("notifyCustomer", Boolean.class, () -> {
            services.notifyCustomer(orderId, customerId);
            return true;
        });

        logger.info("Order {} workflow completed at version {}", orderId, ctx.workflowVersion());
    }
}
