package io.axoniq.multitenancy.api;



import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.io.Serializable;

public class AddCmd implements Serializable {

    @TargetAggregateIdentifier
    private String id;
    private Integer amount;

    public AddCmd() {

    }

    public AddCmd(String id, Integer amount) {
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
