package io.axoniq.dev.samples.sequencingpolicy;

import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightCanceledEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightId;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlightIdSequencingPolicyTest {

    private final FlightIdSequencingPolicy testSubject = new FlightIdSequencingPolicy();

    @Test
    void returnsEmptyOptionalForNoneFlightEvent() {
        EventMessage testEvent = new GenericEventMessage(new MessageType(String.class), "some-event");

        assertEquals(Optional.empty(), testSubject.sequenceIdentifierFor(testEvent, null));
    }

    @Test
    void returnsFlightIdForFlightEventImplementations() {
        FlightId expectedResult = new FlightId(UUID.randomUUID().toString());

        FlightCanceledEvent testEvent = new FlightCanceledEvent(expectedResult);
        EventMessage testEventMessage = new GenericEventMessage(new MessageType(testEvent.getClass()), testEvent);

        assertEquals(Optional.of(expectedResult), testSubject.sequenceIdentifierFor(testEventMessage, null));
    }
}
