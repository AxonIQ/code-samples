package io.axoniq.dev.samples.payment.api;

import io.axoniq.dev.samples.uuid.PaymentId;

public class OrderPaymentCancelledEvent {

    PaymentId paymentId;

    public OrderPaymentCancelledEvent(PaymentId paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }
}
