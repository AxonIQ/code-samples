package io.axoniq.dev.samples.api.events;

import java.util.UUID;

public record UserRemovedEvent(
        UUID userId
) {

}
