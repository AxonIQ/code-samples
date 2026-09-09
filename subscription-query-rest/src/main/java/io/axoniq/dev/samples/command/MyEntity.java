package io.axoniq.dev.samples.command;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;

@EventSourced(tagKey = "MyEntity")
class MyEntity {

    private String entityId;

    @EntityCreator
    public MyEntity() {
        // Required by Axon Framework
    }

    @CommandHandler
    public static void handle(CreateMyEntityCommand command, EventAppender eventAppender) {
        eventAppender.append(new MyEntityCreatedEvent(command.entityId()));
    }

    @EventSourcingHandler
    public void on(MyEntityCreatedEvent event) {
        entityId = event.entityId();
    }
}
