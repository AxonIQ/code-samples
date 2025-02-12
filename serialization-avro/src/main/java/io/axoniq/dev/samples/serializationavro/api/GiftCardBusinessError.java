package io.axoniq.dev.samples.serializationavro.api;

import javax.annotation.Nonnull;

public record GiftCardBusinessError(
        String name,
        @Nonnull GiftCardBusinessErrorCode code,
        String message
) {

}
