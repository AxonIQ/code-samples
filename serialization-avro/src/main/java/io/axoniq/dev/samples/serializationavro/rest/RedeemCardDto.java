package io.axoniq.dev.samples.serializationavro.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RedeemCardDto(@JsonProperty("amount") int amount) {

}
