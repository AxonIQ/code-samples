package io.axoniq.distributedexceptions.command;

import io.axoniq.distributedexceptions.api.GiftCardBusinessErrorCode;

/**
 * Domain specific exception
 *
 * @author Stefan Andjelkovic
 */
class GiftCardException extends RuntimeException {
    private String errorMessage;
    private GiftCardBusinessErrorCode errorCode;

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

/**
 * @author Stefan Andjelkovic
 */
class InsufficientFunds extends GiftCardException {
    public InsufficientFunds() {
        super("amount > remaining value", GiftCardBusinessErrorCode.INSUFFICIENT_FUNDS);
    }
    public InsufficientFunds(String errorMessage) {
        super(errorMessage, GiftCardBusinessErrorCode.INSUFFICIENT_FUNDS);
    }
}

/**
 * @author Stefan Andjelkovic
 */
class NegativeOrZeroAmount extends GiftCardException {
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
