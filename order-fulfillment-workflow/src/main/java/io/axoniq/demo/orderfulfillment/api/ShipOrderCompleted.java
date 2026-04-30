package io.axoniq.demo.orderfulfillment.api;

/**
 * Emitted automatically by the workflow engine when the {@code shipOrder} step completes.
 * The Completed event's payload is whatever the action returned — here {@code orderId},
 * {@code trackingNumber} and the route coordinates so downstream simulators can animate
 * truck movement without re-reading the projection.
 */
public record ShipOrderCompleted(
        String orderId,
        String trackingNumber,
        double originLat,
        double originLng,
        double destinationLat,
        double destinationLng
) {
}
