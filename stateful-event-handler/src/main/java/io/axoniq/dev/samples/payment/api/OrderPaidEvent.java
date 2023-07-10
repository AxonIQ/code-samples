package io.axoniq.dev.samples.payment.api;

import io.axoniq.dev.samples.uuid.PaymentId;

public record OrderPaidEvent(
        PaymentId paymentId
) {

}