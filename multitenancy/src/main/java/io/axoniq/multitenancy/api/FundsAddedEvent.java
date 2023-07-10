package io.axoniq.multitenancy.api;

import javax.annotation.Nonnull;

public record FundsAddedEvent(@Nonnull String id, int amount) {

}
