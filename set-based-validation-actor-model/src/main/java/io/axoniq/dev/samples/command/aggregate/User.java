package io.axoniq.dev.samples.command.aggregate;

import io.axoniq.dev.samples.api.commands.RemoveUser;
import io.axoniq.dev.samples.api.events.UserRegistered;
import io.axoniq.dev.samples.api.events.UserRemoved;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.UUID;

@Aggregate
public class User {

    @AggregateIdentifier
    public UUID userId;

    public User(UUID userId, String emailAddress) {
        AggregateLifecycle.apply(new UserRegistered(userId, emailAddress));
    }

    @CommandHandler
    public void handle(RemoveUser command) {
        AggregateLifecycle.apply(new UserRemoved(command.getUserId()));
    }

    @EventSourcingHandler
    public void handle(UserRegistered event) {
        this.userId = event.getUserId();
    }

    @EventSourcingHandler
    public void handle(UserRemoved event) {
        AggregateLifecycle.markDeleted();
    }

    public User() {
        //needed by Axon
    }
}
