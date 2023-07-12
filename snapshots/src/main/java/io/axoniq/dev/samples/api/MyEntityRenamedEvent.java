package io.axoniq.dev.samples.api;

public record MyEntityRenamedEvent(
        String entityId,
        String name
) {

}
