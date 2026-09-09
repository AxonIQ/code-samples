package io.axoniq.dev.samples.command;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import io.axoniq.dev.samples.api.MyEntityRenamedEvent;
import io.axoniq.dev.samples.api.RenameMyEntityCommand;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.Snapshotting;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@EventSourced(tagKey = "MyEntity")
@Snapshotting(afterEvents = 5)
class MyEntity {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String entityId;
    private String name;

    @CommandHandler
    public static void handle(CreateMyEntityCommand command, EventAppender appender) {
        logger.info("[CreateMyEntityCommand] Entity with id [{}] and name [{}] created.",
                    command.entityId(), command.name());

        appender.append(new MyEntityCreatedEvent(command.entityId(), command.name()));
    }

    @CommandHandler
    public void on(RenameMyEntityCommand command, EventAppender appender) {
        logger.info("[RenameMyEntityCommand] Entity with id [{}] and name [{}] updated.",
                    command.entityId(), command.name());

        if (name.equals(command.name())) {
            throw new IllegalArgumentException("New name can not be the same as current name.");
        }
        appender.append(new MyEntityRenamedEvent(command.entityId(), command.name()));
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

    // The general Converter defaults to a JacksonConverter, so the constructed Snapshot will also be
    // (de)serialized through Jackson.
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

    @EntityCreator
    public MyEntity() {
        // Required by Axon Framework
    }
}
