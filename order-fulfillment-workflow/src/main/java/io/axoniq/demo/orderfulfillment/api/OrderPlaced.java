package io.axoniq.demo.orderfulfillment.api;

import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record OrderPlaced(
        String orderId,
        String customerId,
        String email,
        double amount,
        String originCity,
        double originLat,
        double originLng,
        String destinationCity,
        double destinationLat,
        double destinationLng,
        String scenario
) {
}
