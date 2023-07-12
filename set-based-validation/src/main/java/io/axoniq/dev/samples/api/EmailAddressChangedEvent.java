package io.axoniq.dev.samples.api;

import java.util.UUID;

public record EmailAddressChangedEvent(UUID accountId, String emailAddress) {

}
