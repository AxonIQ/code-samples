package io.axoniq.dev.samples.api.events;

import java.util.Objects;
import java.util.UUID;

public class UserRemoved {
    private final UUID userId;

    public UserRemoved(UUID userId) {
        this.userId = userId;
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
        UserRemoved that = (UserRemoved) o;
        return Objects.equals(getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId());
    }

    @Override
    public String toString() {
        return "UserRemoved{" +
                "userId=" + userId +
                '}';
    }
}
