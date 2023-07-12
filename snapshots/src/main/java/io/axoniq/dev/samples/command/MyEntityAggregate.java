package io.axoniq.dev.samples.command;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
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

@Aggregate(snapshotTriggerDefinition = "mySnapshotTriggerDefinition")
class MyEntityAggregate {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String entityId;
    private String name;

    @CommandHandler
    public MyEntityAggregate(CreateMyEntityCommand command) {
        logger.info("[CreateMyEntityCommand] Entity with id [{}] and name [{}] created.",
                    command.entityId(), command.name());

        apply(new MyEntityCreatedEvent(command.entityId(), command.name()));
    }

    @CommandHandler
    public void on(RenameMyEntityCommand command) {
        logger.info("[RenameMyEntityCommand] Entity with id [{}] and name [{}] updated.",
                    command.entityId(), command.name());

        if (name.equals(command.name())) {
            throw new IllegalArgumentException("New name can not be the same as current name.");
        }
        apply(new MyEntityRenamedEvent(command.entityId(), command.name()));
    }

    @EventSourcingHandler
    public void on(MyEntityCreatedEvent event) {
        entityId = event.entityId();
        name = event.name();

        logger.info("[MyEntityCreatedEvent] Entity with id [{}] being event sourced.", event.entityId());
    }

    @EventSourcingHandler
    public void on(MyEntityRenamedEvent event) {
        name = event.name();

        logger.info("[MyEntityRenamedEvent] Entity with id [{}] being event sourced.", event.entityId());
    }

    // Since the main Serializer is a JacksonSerializer, the constructed Snapshot will also be serialized through Jackson
    @JsonGetter
    String getEntityId() {
        return entityId;
    }

    @JsonSetter
    void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @JsonGetter
    String getName() {
        return name;
    }

    @JsonSetter
    void setName(String name) {
        this.name = name;
    }

    public MyEntityAggregate() {
        // Required by Axon Framework
    }
}
