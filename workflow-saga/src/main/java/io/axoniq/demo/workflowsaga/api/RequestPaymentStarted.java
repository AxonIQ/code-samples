package io.axoniq.demo.workflowsaga.api;

/**
 * Emitted automatically by the workflow engine when the {@code requestPayment} step starts.
 * No code publishes this — it is the Started event of the {@code awaitExecute("requestPayment", ...)}
 * primitive, with the step's input payload.
 */
public record RequestPaymentStarted(String orderId, String paymentId) {
}
