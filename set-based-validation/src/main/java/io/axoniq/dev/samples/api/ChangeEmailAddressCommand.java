package io.axoniq.dev.samples.api;

import java.util.Objects;
import java.util.UUID;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class ChangeEmailAddressCommand {
    @TargetAggregateIdentifier
    private UUID accountId;
    private String updatedEmailAddress;

    public ChangeEmailAddressCommand(UUID accountId, String updatedEmailAddress) {
        this.accountId = accountId;
        this.updatedEmailAddress = updatedEmailAddress;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getUpdatedEmailAddress() {
        return updatedEmailAddress;
    }

    public void setUpdatedEmailAddress(String updatedEmailAddress) {
        this.updatedEmailAddress = updatedEmailAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeEmailAddressCommand that = (ChangeEmailAddressCommand) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(updatedEmailAddress, that.updatedEmailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, updatedEmailAddress);
    }
}
