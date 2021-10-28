package io.axoniq.dev.samples.sequencingpolicy;

import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightCanceledEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightId;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlightIdSequencingPolicyTest {

    private final FlightIdSequencingPolicy testSubject = new FlightIdSequencingPolicy();

    @Test
    void testReturnsNullForNoneFlightEvent() {
        EventMessage<Object> testEvent = GenericEventMessage.asEventMessage("some-event");

        assertNull(testSubject.getSequenceIdentifierFor(testEvent));
    }

    @Test
    void testReturnsFlightIdForFlightEventImplementations() {
        FlightId expectedResult = new FlightId(UUID.randomUUID().toString());

        FlightCanceledEvent testEvent = new FlightCanceledEvent(expectedResult);
        EventMessage<Object> testEventMessage = GenericEventMessage.asEventMessage(testEvent);

        assertEquals(expectedResult, testSubject.getSequenceIdentifierFor(testEventMessage));
    }
}