package io.axoniq.dev.samples.api.events;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email
) {

}
