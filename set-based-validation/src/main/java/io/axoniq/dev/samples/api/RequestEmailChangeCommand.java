package io.axoniq.dev.samples.api;

import org.axonframework.commandhandling.RoutingKey;

import java.util.UUID;

/**
 * Since this command is routed to a command handling component, the RoutingKey annotation is used. The
 * TargetAggregateIdentifier could also be used because that has the RoutingKey meta annotated
 */
public record RequestEmailChangeCommand(
        @RoutingKey UUID accountId,
        String updatedEmailAddress
) {

}