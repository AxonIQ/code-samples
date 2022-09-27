package io.axoniq.multitenancy.api;



import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.io.Serializable;

public class IssueCmd implements Serializable {

    @TargetAggregateIdentifier
    private String id;
    private Integer amount;

    public IssueCmd() {

    }

    public IssueCmd(String id, Integer amount) {
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
