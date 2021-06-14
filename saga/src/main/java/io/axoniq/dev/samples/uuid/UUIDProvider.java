package io.axoniq.dev.samples.uuid;

import org.springframework.stereotype.Component;

@Component
public class UUIDProvider {

    public OrderId generateOrderId() {
        return new OrderId();
    }

    public PaymentId generatePaymentId() {
        return new PaymentId();
    }

    public ShipmentId generateShipmentId() {
        return new ShipmentId();
    }
}
