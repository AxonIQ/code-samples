package io.axoniq.dev.samples.serializationavro.api;

public class InsufficientFunds extends GiftCardException {

    public InsufficientFunds() {
        super("amount > remaining value", GiftCardBusinessErrorCode.INSUFFICIENT_FUNDS);
    }

    public InsufficientFunds(String errorMessage) {
        super(errorMessage, GiftCardBusinessErrorCode.INSUFFICIENT_FUNDS);
    }
}
