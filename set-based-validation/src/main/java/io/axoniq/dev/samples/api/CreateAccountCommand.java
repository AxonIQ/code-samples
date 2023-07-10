package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

public record CreateAccountCommand(
        @TargetAggregateIdentifier UUID accountId,
        String emailAddress
) {

}
