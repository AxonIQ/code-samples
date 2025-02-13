package io.axoniq.dev.samples.serializationavro.api;

public class NegativeOrZeroAmount extends GiftCardException {

    private int amount;

    public NegativeOrZeroAmount(int amount) {
        super("amount $amount <=0", GiftCardBusinessErrorCode.NEGATIVE_AMOUNT);
    }

    public NegativeOrZeroAmount(int amount, String errorMessage) {
        super(errorMessage, GiftCardBusinessErrorCode.NEGATIVE_AMOUNT);
    }

    public int getAmount() {
        return amount;
    }
}
