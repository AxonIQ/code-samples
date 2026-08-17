package io.axoniq.dev.samples.payment.api;

import io.axoniq.dev.samples.uuid.PaymentId;
import org.axonframework.modelling.annotation.TargetEntityId;

public record PayOrderCommand(
        @TargetEntityId PaymentId paymentId
) {

}
