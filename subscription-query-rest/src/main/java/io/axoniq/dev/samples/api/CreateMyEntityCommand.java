package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record CreateMyEntityCommand(
        @TargetAggregateIdentifier String entityId
) {

}
