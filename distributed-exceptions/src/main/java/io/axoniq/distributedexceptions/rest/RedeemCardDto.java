package io.axoniq.distributedexceptions.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RedeemCardDto(@JsonProperty("amount") int amount) {

}
