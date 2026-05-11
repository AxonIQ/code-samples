package io.axoniq.demo.orderfulfillment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendConfirmation(String email, String trackingNumber) {
        logger.info("Sending order confirmation to {} (tracking number {}).", email, trackingNumber);
    }
}
