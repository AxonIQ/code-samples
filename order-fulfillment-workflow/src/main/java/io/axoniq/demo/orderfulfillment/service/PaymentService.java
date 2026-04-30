package io.axoniq.demo.orderfulfillment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public void initiatePayment(Map<String, Object> payload) {
        logger.info("Initiating payment for customer {} (amount {}).",
                    payload.get("customerId"), payload.get("amount"));
    }
}
