package io.axoniq.distributedexceptions.api;

import org.jetbrains.annotations.NotNull;

public class RedeemedEvt {

    @NotNull
    private final String id;
    private final int amount;

    public RedeemedEvt(@NotNull String id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }
}
