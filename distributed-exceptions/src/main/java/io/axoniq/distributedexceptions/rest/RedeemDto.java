package io.axoniq.distributedexceptions.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Stefan Andjelkovic
 */
public class RedeemDto {
    private final int amount;

    public RedeemDto(@JsonProperty("amount") int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}
