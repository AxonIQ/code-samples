package io.axoniq.dev.samples.command;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class MyEntityAggregate {

    @AggregateIdentifier
    private String entityId;

    public MyEntityAggregate() {
        // Required by Axon Framework
    }

    @CommandHandler
    public MyEntityAggregate(CreateMyEntityCommand command) {
        apply(new MyEntityCreatedEvent(command.entityId()));
    }

    @EventSourcingHandler
    public void on(MyEntityCreatedEvent event) {
        entityId = event.entityId();
    }
}
