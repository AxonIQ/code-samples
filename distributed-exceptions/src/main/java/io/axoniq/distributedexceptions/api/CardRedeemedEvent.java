package io.axoniq.distributedexceptions.api;

import javax.annotation.Nonnull;

public record CardRedeemedEvent(@Nonnull String id, int amount) {

}
