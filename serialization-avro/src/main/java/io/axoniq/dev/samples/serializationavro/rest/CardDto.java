package io.axoniq.dev.samples.serializationavro.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueCardDto(@JsonProperty("amount") int amount) {

}
