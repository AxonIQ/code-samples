package io.axoniq.demo.orderfulfillment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ShippingService {

    private static final Logger logger = LoggerFactory.getLogger(ShippingService.class);

    public Map<String, Object> shipOrder(Map<String, Object> payload) {
        var orderId = (String) payload.get("orderId");
        var trackingNumber = (String) payload.get("trackingNumber");
        logger.info("Shipping order {} with tracking number {}.", orderId, trackingNumber);
        return Map.of(
                "orderId", orderId,
                "trackingNumber", trackingNumber,
                "originLat", payload.get("originLat"),
                "originLng", payload.get("originLng"),
                "destinationLat", payload.get("destinationLat"),
                "destinationLng", payload.get("destinationLng")
        );
    }
}
