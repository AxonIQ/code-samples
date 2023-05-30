package io.axoniq.dev.samples.order.api;

import io.axoniq.dev.samples.uuid.OrderId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class CompleteOrderProcessCommand {

    @TargetAggregateIdentifier
    OrderId orderId;

    Boolean isPaid;

    Boolean orderIsDelivered;

    public CompleteOrderProcessCommand(OrderId orderId, Boolean isPaid, Boolean orderIsDelivered) {
        this.orderId = orderId;
        this.isPaid = isPaid;
        this.orderIsDelivered = orderIsDelivered;
    }
}
