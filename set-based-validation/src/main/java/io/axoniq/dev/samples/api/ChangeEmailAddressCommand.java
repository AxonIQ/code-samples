package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Objects;
import java.util.UUID;

public class ChangeEmailAddressCommand {

    @TargetAggregateIdentifier
    private final UUID accountId;
    private final String updatedEmailAddress;

    public ChangeEmailAddressCommand(UUID accountId, String updatedEmailAddress) {
        this.accountId = accountId;
        this.updatedEmailAddress = updatedEmailAddress;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getUpdatedEmailAddress() {
        return updatedEmailAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChangeEmailAddressCommand that = (ChangeEmailAddressCommand) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(updatedEmailAddress, that.updatedEmailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, updatedEmailAddress);
    }

    @Override
    public String toString() {
        return "ChangeEmailAddressCommand{" +
                "accountId=" + accountId +
                ", updatedEmailAddress='" + updatedEmailAddress + '\'' +
                '}';
    }
}
