package io.axoniq.demo.orderfulfillment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    public boolean reserveStock(Map<String, Object> payload) {
        logger.info("Reserving stock for customer {} (amount {}).",
                    payload.get("customerId"), payload.get("amount"));
        return true;
    }
}
