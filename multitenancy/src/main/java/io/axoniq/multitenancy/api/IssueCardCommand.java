package io.axoniq.multitenancy.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record IssueCardCommand(@TargetAggregateIdentifier String id, Integer amount) {

}