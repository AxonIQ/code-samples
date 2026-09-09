package io.axoniq.dev.samples.order.api;

import io.axoniq.dev.samples.uuid.OrderId;
import org.axonframework.modelling.annotation.TargetEntityId;

public record CompleteOrderProcessCommand(
        @TargetEntityId OrderId orderId,
        boolean isPaid,
        boolean orderIsDelivered
) {

}
