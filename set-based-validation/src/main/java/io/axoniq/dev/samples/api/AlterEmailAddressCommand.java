package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

public record AlterEmailAddressCommand(
        @TargetAggregateIdentifier UUID accountId,
        String updatedEmailAddress
) {

}
