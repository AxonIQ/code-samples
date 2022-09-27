package io.axoniq.multitenancy.api;

import java.io.Serializable;

public class IssuedEvt implements Serializable {

    private String id;
    private Integer amount;

    public IssuedEvt(String id, Integer amount) {
        this.id = id;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public Integer getAmount() {
        return amount;
    }
}
