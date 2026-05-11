package io.axoniq.demo.orderfulfillment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    public boolean reserveStock(Map<String, Object> payload) {
        var customerId = payload.get("customerId");
        var amount = payload.get("amount");
        var scenario = (String) payload.get("scenario");
        if ("out-of-stock".equals(scenario)) {
            logger.info("Stock unavailable (forced) for customer {} (amount {}).", customerId, amount);
            return false;
        }
        logger.info("Reserving stock for customer {} (amount {}).", customerId, amount);
        return true;
    }
}
