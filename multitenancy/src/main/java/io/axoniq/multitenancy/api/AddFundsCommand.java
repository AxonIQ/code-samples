package io.axoniq.multitenancy.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import javax.annotation.Nonnull;

public record AddFundsCommand(@TargetAggregateIdentifier @Nonnull String id, Integer amount) {

}
