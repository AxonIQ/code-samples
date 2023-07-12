package io.axoniq.distributedexceptions.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import javax.annotation.Nonnull;

public record IssueCardCommand(@TargetAggregateIdentifier @Nonnull String id, int amount) {

}
