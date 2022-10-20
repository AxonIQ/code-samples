package io.axoniq.dev.samples.api.events;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Objects;
import java.util.UUID;

public class UserRegistered {

    private final UUID userId;

    private final String email;

    public UserRegistered(UUID accountID, String email) {
        this.userId = accountID;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserRegistered that = (UserRegistered) o;
        return Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getEmail(),
                                                                               that.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getEmail());
    }

    @Override
    public String toString() {
        return "CreateAccount{" +
                "accountID=" + userId +
                ", email='" + email + '\'' +
                '}';
    }
}
