package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Objects;
import java.util.UUID;

public class CreateAccountCommand {

    @TargetAggregateIdentifier
    private final UUID accountId;
    private final String emailAddress;

    public CreateAccountCommand(UUID accountId, String emailAddress) {
        this.accountId = accountId;
        this.emailAddress = emailAddress;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreateAccountCommand that = (CreateAccountCommand) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(emailAddress, that.emailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, emailAddress);
    }

    @Override
    public String toString() {
        return "CreateAccountCommand{" +
                "accountId=" + accountId +
                ", emailAddress='" + emailAddress + '\'' +
                '}';
    }
}
