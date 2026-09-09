package io.axoniq.distributedexceptions.api;

import org.axonframework.messaging.commandhandling.annotation.Command;
import org.axonframework.modelling.annotation.TargetEntityId;

@Command(routingKey = "id")
public record RedeemCardCommand(@TargetEntityId String id, int amount) {

}
