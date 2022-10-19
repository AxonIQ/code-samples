package io.axoniq.dev.samples.api.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Objects;
import java.util.UUID;

public class RemoveUserWithUniqueEmailAddress {

    @TargetAggregateIdentifier
    private final String email;

    private final UUID userId;


    public RemoveUserWithUniqueEmailAddress(String email, UUID accountID) {
        this.email = email;
        this.userId = accountID;
    }

    public String getEmail() {
        return email;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RemoveUserWithUniqueEmailAddress that = (RemoveUserWithUniqueEmailAddress) o;
        return Objects.equals(getEmail(), that.getEmail()) && Objects.equals(getUserId(),
                                                                             that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEmail(), getUserId());
    }

    @Override
    public String toString() {
        return "CreateAccountWithUniqueEmailAddress{" +
                "email='" + email + '\'' +
                ", accountID=" + userId +
                '}';
    }
}
