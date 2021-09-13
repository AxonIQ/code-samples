package io.axoniq.distributedexceptions.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Stefan Andjelkovic
 */
public class IssueNewGiftCardDto {
    private final int amount;

    @JsonCreator
    public IssueNewGiftCardDto(@JsonProperty("amount")  int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}
