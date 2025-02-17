package io.axoniq.dev.samples.serializationavro.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardDto(@JsonProperty("id") String id, @JsonProperty("amount") int amount) {

}
