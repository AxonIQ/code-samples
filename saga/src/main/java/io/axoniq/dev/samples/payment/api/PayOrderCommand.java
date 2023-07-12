package io.axoniq.dev.samples.payment.api;

import io.axoniq.dev.samples.uuid.PaymentId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record PayOrderCommand(@TargetAggregateIdentifier PaymentId paymentId) {

}
