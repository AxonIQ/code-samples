package io.axoniq.dev.sample.api;

import java.beans.ConstructorProperties;
import java.util.Objects;
import java.util.UUID;

/**
 * {@link AccountEvent} dedicated towards creating an account.
 *
 * @author Steven van Beelen
 */
public class AccountCreatedEvent implements AccountEvent {

    private final UUID accountId;

    @ConstructorProperties({"accountId"})
    public AccountCreatedEvent(UUID accountId) {
        this.accountId = accountId;
    }

    @Override
    public UUID getAccountId() {
        return accountId;
    }

    @Override
    public String accountIdString() {
        return accountId.toString();
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
        return Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return "AccountCreatedEvent{" +
                "accountId=" + accountId +
                '}';
    }
}
