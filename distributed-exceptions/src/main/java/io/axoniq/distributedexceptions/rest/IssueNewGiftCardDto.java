package io.axoniq.distributedexceptions.rest;

/**
 * @author Stefan Andjelkovic
 */
public class IssueNewGiftCardDto {
    private final int amount;

    public IssueNewGiftCardDto(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}
