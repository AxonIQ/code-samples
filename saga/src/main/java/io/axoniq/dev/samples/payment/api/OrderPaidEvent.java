package io.axoniq.dev.samples.payment.api;

import io.axoniq.dev.samples.uuid.PaymentId;

public class OrderPaidEvent {

    PaymentId paymentId;

    public OrderPaidEvent(PaymentId paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }
}
