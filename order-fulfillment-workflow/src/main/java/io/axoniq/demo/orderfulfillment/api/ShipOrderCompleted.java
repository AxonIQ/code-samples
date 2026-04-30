package io.axoniq.demo.orderfulfillment.api;

/**
 * Emitted automatically by the workflow engine when the {@code shipOrder} step completes.
 * The Completed event's payload is whatever the action returned — here {@code orderId} and
 * {@code trackingNumber}. The workflow does not publish this event itself.
 */
public record ShipOrderCompleted(String orderId, String trackingNumber) {
}
