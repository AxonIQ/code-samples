package io.axoniq.multitenancy.api;

import java.io.Serializable;

public class AddedEvt implements Serializable {

    private String id;
    private Integer amount;

    public AddedEvt() {

    }

    public AddedEvt(String id, Integer amount) {
        this.id = id;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public void setId(String id) {
        this.id = id;
    }
}
