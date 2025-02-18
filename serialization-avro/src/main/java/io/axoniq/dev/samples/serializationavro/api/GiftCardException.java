package io.axoniq.dev.samples.serializationavro.api;


/**
 * Domain specific exception
 *
 * @author Stefan Andjelkovic
 */
public class GiftCardException extends RuntimeException {

    public enum GiftCardBusinessErrorCode {
        NEGATIVE_AMOUNT,
        INSUFFICIENT_FUNDS,
        UNKNOWN
    }

    private final String errorMessage;
    private final GiftCardBusinessErrorCode errorCode;

    public GiftCardException(String errorMessage, GiftCardBusinessErrorCode errorCode) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public GiftCardBusinessErrorCode getErrorCode() {
        return errorCode;
    }
}

