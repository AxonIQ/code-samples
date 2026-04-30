package io.axoniq.demo.workflowsaga.api;

/**
 * Emitted automatically by the workflow engine when the {@code requestShipment} step starts.
 */
public record RequestShipmentStarted(String orderId, String shipmentId) {
}
