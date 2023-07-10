package io.axoniq.dev.samples.api.events;

import java.util.UUID;

public record EmailAddressApprovedEvent(
        String emailAddress,
        UUID userId
) {
}
