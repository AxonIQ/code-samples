package io.axoniq.dev.samples.api.events;

import java.util.Objects;
import java.util.UUID;

public class EmailAddressRemoved {

    private final String emailAddress;


    private final UUID userId;

    public EmailAddressRemoved(String emailAddress, UUID userId) {
        this.emailAddress = emailAddress;
        this.userId = userId;
    }

    public String getEmailAddress() {
        return emailAddress;
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
        EmailAddressRemoved that = (EmailAddressRemoved) o;
        return Objects.equals(getEmailAddress(), that.getEmailAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEmailAddress());
    }

    @Override
    public String toString() {
        return "EmailAddressRemoved{" +
                "emailAddress='" + emailAddress + '\'' +
                '}';
    }
}
