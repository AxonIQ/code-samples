package io.axoniq.demo.versioning.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tiny "service layer" the workflow steps invoke. Each operation logs and returns a deterministic
 * result so the demo's behavior is easy to follow in the console.
 */
@Component
public class DemoServices {

    private static final Logger logger = LoggerFactory.getLogger(DemoServices.class);

    public boolean reserveStock(String orderId) {
        logger.info("[reserveStock] reserving stock for order {}", orderId);
        return true;
    }

    public boolean validateAddress(String orderId) {
        logger.info("[validateAddress] validating shipping address for order {}", orderId);
        return true;
    }

    public boolean fraudReview(String orderId) {
        logger.info("[fraudReview] running fraud review for order {}", orderId);
        return true;
    }

    public boolean chargePayment(String orderId, int amount) {
        logger.info("[chargePayment] charging ${} for order {}", amount, orderId);
        return true;
    }

    public void notifyCustomer(String orderId, String customerId) {
        logger.info("[notifyCustomer] notifying customer {} about order {}", customerId, orderId);
    }
}
