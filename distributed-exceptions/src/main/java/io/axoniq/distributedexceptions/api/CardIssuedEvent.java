package io.axoniq.distributedexceptions.api;

import javax.annotation.Nonnull;

public record CardIssuedEvent(@Nonnull String id, int amount) {

}
