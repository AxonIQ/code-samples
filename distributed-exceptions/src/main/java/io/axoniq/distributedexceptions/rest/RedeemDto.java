package io.axoniq.distributedexceptions.rest;

/**
 * @author Stefan Andjelkovic
 */
public class RedeemDto {
    private final int amount;

    public RedeemDto(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}
