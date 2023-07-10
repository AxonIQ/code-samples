package io.axoniq.distributedexceptions.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueCardDto(@JsonProperty("amount") int amount) {

}
