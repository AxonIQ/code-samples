package io.axoniq.distributedexceptions.api;

import javax.annotation.Nonnull;

public class GiftCardBusinessError {

    private final String name;
    @Nonnull
    private final GiftCardBusinessErrorCode code;
    private final String message;

    public GiftCardBusinessError(String name, @Nonnull GiftCardBusinessErrorCode code, String message) {
        this.name = name;
        this.code = code;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    @Nonnull
    public GiftCardBusinessErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
