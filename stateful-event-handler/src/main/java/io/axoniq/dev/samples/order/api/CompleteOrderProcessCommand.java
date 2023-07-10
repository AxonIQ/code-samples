package io.axoniq.dev.samples.order.api;

import io.axoniq.dev.samples.uuid.OrderId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record CompleteOrderProcessCommand(
        @TargetAggregateIdentifier OrderId orderId,
        boolean isPaid,
        boolean orderIsDelivered
) {

}
