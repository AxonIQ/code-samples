package io.axoniq.dev.samples.api;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountCreatedEvent {
    private UUID accountId;
    private String emailAddress;

    public AccountCreatedEvent(@JsonProperty("accountId")UUID accountId, @JsonProperty("emailAddress") String emailAddress){
        this.accountId = accountId;
        this.emailAddress = emailAddress;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountCreatedEvent that = (AccountCreatedEvent) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(emailAddress, that.emailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, emailAddress);
    }
}
