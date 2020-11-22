package io.axoniq.dev.samples.command;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import io.axoniq.dev.samples.api.MyEntityRenamedEvent;
import io.axoniq.dev.samples.api.RenameMyEntityCommand;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * @author Sara Pellegrini
 * @author Lucas Campos
 */
@Aggregate(snapshotTriggerDefinition = "mySnapshotTriggerDefinition")
public class MyEntityAggregate {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String entityId;

    private String name;

    public MyEntityAggregate() {
    }

    @CommandHandler
    public MyEntityAggregate(CreateMyEntityCommand command) {
        logger.info("[CreateMyEntityCommand] Entity with id [{}] and name [{}] created.",
                    command.getEntityId(),
                    command.getName());
        apply(new MyEntityCreatedEvent(command.getEntityId(), command.getName()));
    }

    @CommandHandler
    public void on(RenameMyEntityCommand command) {
        logger.info("[RenameMyEntityCommand] Entity with id [{}] and name [{}] updated.",
                    command.getEntityId(),
                    command.getName());
        if (name.equals(command.getName())) {
            throw new IllegalArgumentException("New name can not be the same as current name.");
        }
        apply(new MyEntityRenamedEvent(command.getEntityId(), command.getName()));
    }

    @EventSourcingHandler
    public void on(MyEntityCreatedEvent event) {
        entityId = event.getEntityId();
        name = event.getName();
        logger.info("[MyEntityCreatedEvent] Entity with id [{}] being event sourced.", event.getEntityId());
    }

    @EventSourcingHandler
    public void on(MyEntityRenamedEvent event) {
        name = event.getName();
        logger.info("[MyEntityRenamedEvent] Entity with id [{}] being event sourced.", event.getEntityId());
    }
}
