package io.axoniq.demo.orderfulfillment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ShippingService {

    private static final Logger logger = LoggerFactory.getLogger(ShippingService.class);

    public Map<String, Object> shipOrder(Map<String, Object> payload) {
        var orderId = (String) payload.get("orderId");
        var trackingNumber = "TRK-" + UUID.randomUUID();
        logger.info("Shipping order {} with tracking number {}.", orderId, trackingNumber);
        return Map.of("orderId", orderId, "trackingNumber", trackingNumber);
    }
}
