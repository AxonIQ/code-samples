package io.axoniq.dev.samples.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class AccountCreatedEvent {

    private final UUID accountId;
    private final String emailAddress;

    public AccountCreatedEvent(@JsonProperty("accountId") UUID accountId,
                               @JsonProperty("emailAddress") String emailAddress) {
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
        AccountCreatedEvent that = (AccountCreatedEvent) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(emailAddress, that.emailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, emailAddress);
    }

    @Override
    public String toString() {
        return "AccountCreatedEvent{" +
                "accountId=" + accountId +
                ", emailAddress='" + emailAddress + '\'' +
                '}';
    }
}
