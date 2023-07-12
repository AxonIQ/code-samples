package io.axoniq.distributedexceptions.api;

import javax.annotation.Nonnull;

public record GiftCardBusinessError(
        String name,
        @Nonnull GiftCardBusinessErrorCode code,
        String message
) {

}
