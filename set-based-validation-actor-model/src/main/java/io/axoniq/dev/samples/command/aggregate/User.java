package io.axoniq.dev.samples.command.aggregate;

import io.axoniq.dev.samples.api.commands.RemoveUserCommand;
import io.axoniq.dev.samples.api.events.UserRegisteredEvent;
import io.axoniq.dev.samples.api.events.UserRemovedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.UUID;

@Aggregate
class User {

    @AggregateIdentifier
    public UUID userId;

    public User(UUID userId, String emailAddress) {
        AggregateLifecycle.apply(new UserRegisteredEvent(userId, emailAddress));
    }

    @CommandHandler
    public void handle(RemoveUserCommand command) {
        AggregateLifecycle.apply(new UserRemovedEvent(command.userId()));
    }

    @EventSourcingHandler
    public void handle(UserRegisteredEvent event) {
        this.userId = event.userId();
    }

    @EventSourcingHandler
    public void handle(UserRemovedEvent event) {
        AggregateLifecycle.markDeleted();
    }

    public User() {
        //needed by Axon
    }
}
