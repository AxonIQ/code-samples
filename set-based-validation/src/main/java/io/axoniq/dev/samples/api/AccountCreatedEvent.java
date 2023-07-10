package io.axoniq.dev.samples.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AccountCreatedEvent(
        @JsonProperty("accountId") UUID accountId,
        @JsonProperty("emailAddress") String emailAddress
) {

}
