package io.axoniq.dev.samples.api;

public record MyEntityCreatedEvent(
        String entityId,
        String name
) {

}
