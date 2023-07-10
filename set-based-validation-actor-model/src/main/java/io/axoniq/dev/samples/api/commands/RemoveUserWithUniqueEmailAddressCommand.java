package io.axoniq.dev.samples.api.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

public record RemoveUserWithUniqueEmailAddressCommand(
        @TargetAggregateIdentifier String email,
        UUID userId
) {

}