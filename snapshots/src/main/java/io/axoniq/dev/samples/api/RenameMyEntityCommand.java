package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record RenameMyEntityCommand(
        @TargetAggregateIdentifier String entityId,
        String name
) {

}
