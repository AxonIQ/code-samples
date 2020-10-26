package io.axoniq.dev.samples.command;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import io.axoniq.dev.samples.api.MyEntityRenamedEvent;
import io.axoniq.dev.samples.api.RenameMyEntityCommand;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * @author Sara Pellegrini
 */
@Aggregate(snapshotTriggerDefinition = "mySnapshotTriggerDefinition")
public class MyEntityAggregate {

    @AggregateIdentifier
    private String entityId;

    public MyEntityAggregate() {
    }

    @CommandHandler
    public MyEntityAggregate(CreateMyEntityCommand command) {
        apply(new MyEntityCreatedEvent(command.getEntityId(), command.getName()));
    }

    @CommandHandler
    public void on(RenameMyEntityCommand command) {
        apply(new MyEntityRenamedEvent(command.getEntityId(), command.getName()));
    }

    @EventSourcingHandler
    public void on(MyEntityCreatedEvent event) {
        entityId = event.getEntityId();
    }
}
