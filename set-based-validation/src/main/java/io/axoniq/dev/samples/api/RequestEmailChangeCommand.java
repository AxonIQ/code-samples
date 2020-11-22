package io.axoniq.dev.samples.api;

import org.axonframework.commandhandling.RoutingKey;

import java.util.Objects;
import java.util.UUID;

public class RequestEmailChangeCommand {

    /**
     * Since this command is routed to a command handling component, the RoutingKey annotation is used. The
     * TargetAggregateIdentifier could also be used because that has the RoutingKey meta annotated
     */
    @RoutingKey
    private final UUID accountId;
    private final String updatedEmailAddress;

    public RequestEmailChangeCommand(UUID accountId, String updatedEmailAddress) {
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
        RequestEmailChangeCommand that = (RequestEmailChangeCommand) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(updatedEmailAddress, that.updatedEmailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, updatedEmailAddress);
    }

    @Override
    public String toString() {
        return "RequestEmailChangeCommand{" +
                "accountId=" + accountId +
                ", updatedEmailAddress='" + updatedEmailAddress + '\'' +
                '}';
    }
}
