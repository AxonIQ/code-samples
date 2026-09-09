package io.axoniq.dev.samples.api;

import org.axonframework.eventsourcing.annotation.EventTag;

public record MyEntityCreatedEvent(
        @EventTag(key = "MyEntity") String entityId
) {

}
