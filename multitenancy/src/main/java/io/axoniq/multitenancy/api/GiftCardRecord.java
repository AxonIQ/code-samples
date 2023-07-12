package io.axoniq.multitenancy.api;

public record GiftCardRecord(String id, Integer initialValue, Integer remainingValue, String payload) {

}
