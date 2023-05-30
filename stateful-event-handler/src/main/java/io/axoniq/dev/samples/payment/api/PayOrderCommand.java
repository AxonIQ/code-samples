package io.axoniq.dev.samples.payment.api;

import io.axoniq.dev.samples.uuid.PaymentId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class PayOrderCommand {

    @TargetAggregateIdentifier
    PaymentId paymentId;

    public PayOrderCommand(PaymentId paymentId) {
        this.paymentId = paymentId;
    }
}
