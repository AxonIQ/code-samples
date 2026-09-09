package io.axoniq.distributedexceptions.api;

import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record CardIssuedEvent(@EventTag(key = "GiftCard") String id, int amount) {

}
