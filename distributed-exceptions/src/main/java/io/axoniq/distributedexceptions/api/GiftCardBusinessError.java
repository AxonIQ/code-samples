package io.axoniq.distributedexceptions.api;

import org.jetbrains.annotations.NotNull;

/**
 * @author Stefan Andjelkovic
 */
public class GiftCardBusinessError {

    private final String name;
    @NotNull
    private final GiftCardBusinessErrorCode code;
    private final String message;

    public GiftCardBusinessError(String name, @NotNull GiftCardBusinessErrorCode code, String message) {
        this.name = name;
        this.code = code;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    @NotNull
    public GiftCardBusinessErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
