package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record ValidateMyEntityCommand(
        @TargetAggregateIdentifier String entityId, String email
) {

}
