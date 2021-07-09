package io.axoniq.dev.samples.commandmodel;

import io.axoniq.dev.samples.api.StreamUpdatedEvent;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Scheduled event publisher. Uses the {@link EventGateway} to publish the {@link StreamUpdatedEvent} on set intervals.
 * The events are filled with a {@link UUID}.
 *
 * @author Steven van Beelen
 */
@Component
public class EventPublisher {

    private final EventGateway eventGateway;

    public EventPublisher(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    @Scheduled(initialDelay = 1_000, fixedDelay = 6_000)
    public void publishEvent() {
        eventGateway.publish(new StreamUpdatedEvent(UUID.randomUUID().toString()));
    }
}
