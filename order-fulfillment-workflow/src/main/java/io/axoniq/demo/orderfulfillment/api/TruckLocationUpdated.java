package io.axoniq.demo.orderfulfillment.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

/**
 * Published by the truck-movement simulator while a shipment is in transit. Not part of the
 * workflow — purely a visualization signal driving the live map.
 */
@Event
public record TruckLocationUpdated(
        String orderId,
        double lat,
        double lng,
        double progress
) {
}
