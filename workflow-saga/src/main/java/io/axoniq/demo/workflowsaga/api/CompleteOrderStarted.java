package io.axoniq.demo.workflowsaga.api;

/**
 * Emitted automatically by the workflow engine when the {@code completeOrder} step starts.
 * The Started event's payload carries the {@code orderId}/{@code paid}/{@code delivered}
 * flags the workflow passed in — the workflow does not publish a separate "process completed"
 * event itself.
 */
public record CompleteOrderStarted(String orderId, boolean paid, boolean delivered) {
}
