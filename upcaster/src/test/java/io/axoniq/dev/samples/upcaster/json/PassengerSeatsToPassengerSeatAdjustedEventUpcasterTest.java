package io.axoniq.dev.samples.upcaster.json;

import org.axonframework.eventhandling.EventData;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.upcasting.event.InitialEventRepresentation;
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;
import java.util.stream.Collectors;

import static io.axoniq.dev.samples.upcaster.json.UpcasterTestingUtils.extractFileContentsToString;
import static io.axoniq.dev.samples.upcaster.json.UpcasterTestingUtils.generateDomainEventData;
import static org.junit.jupiter.api.Assertions.*;

class PassengerSeatsToPassengerSeatAdjustedEventUpcasterTest {

    private static final String FROM_PAYLOAD_TYPE = "io.axoniq.dev.samples.api.PassengerSeatsAdjustedEvent";
    private static final String TO_PAYLOAD_TYPE = "io.axoniq.dev.samples.api.PassengerSeatAdjustedEvent";
    private static final String PAYLOAD_REVISION = null;

    private String passengerSeatsAdjustedEvent;

    private Serializer testSerializer;

    private PassengerSeatsToPassengerSeatAdjustedEventUpcaster testSubject;

    @BeforeEach
    void setUp() {
        passengerSeatsAdjustedEvent = extractFileContentsToString("/PassengerSeatsAdjustedEvent.json");

        testSerializer = JacksonSerializer.defaultSerializer();

        testSubject = new PassengerSeatsToPassengerSeatAdjustedEventUpcaster();
    }

    @Test
    void testCanUpcastReturnsTrueForPassengerSeatsAdjustedEventRevisionNull() {
        EventData<?> testEventData =
                generateDomainEventData(FROM_PAYLOAD_TYPE, PAYLOAD_REVISION, passengerSeatsAdjustedEvent);

        assertTrue(testSubject.canUpcast(new InitialEventRepresentation(testEventData, testSerializer)));
    }

    @Test
    void testCanUpcastReturnsFalseForWrongPayloadType() {
        EventData<?> testEventData =
                generateDomainEventData("incorrect-payload-type", PAYLOAD_REVISION, passengerSeatsAdjustedEvent);

        assertFalse(testSubject.canUpcast(new InitialEventRepresentation(testEventData, testSerializer)));
    }

    @Test
    void testCanUpcastReturnsFalseForWrongRevision() {
        EventData<?> testEventData =
                generateDomainEventData(FROM_PAYLOAD_TYPE, "incorrect-revision", passengerSeatsAdjustedEvent);

        assertFalse(testSubject.canUpcast(new InitialEventRepresentation(testEventData, testSerializer)));
    }

    @Test
    void testDoUpcast() throws JSONException {
        String expectedFirstEventJson = extractFileContentsToString("/PassengerSeatAdjustedEventOne.json");
        String expectedSecondEventJson = extractFileContentsToString("/PassengerSeatAdjustedEventTwo.json");
        String expectedThirdEventJson = extractFileContentsToString("/PassengerSeatAdjustedEventThree.json");
        EventData<?> testEventData =
                generateDomainEventData(FROM_PAYLOAD_TYPE, PAYLOAD_REVISION, passengerSeatsAdjustedEvent);

        List<IntermediateEventRepresentation> result =
                testSubject.doUpcast(new InitialEventRepresentation(testEventData, testSerializer))
                           .collect(Collectors.toList());

        assertEquals(3, result.size());
        IntermediateEventRepresentation firstResult = result.get(2);
        assertEquals(TO_PAYLOAD_TYPE, firstResult.getType().getName());
        assertEquals(PAYLOAD_REVISION, firstResult.getType().getRevision());
        JSONAssert.assertEquals(expectedFirstEventJson, firstResult.getData().getData().toString(), true);

        IntermediateEventRepresentation secondResult = result.get(1);
        assertEquals(TO_PAYLOAD_TYPE, secondResult.getType().getName());
        assertEquals(PAYLOAD_REVISION, secondResult.getType().getRevision());
        JSONAssert.assertEquals(expectedSecondEventJson, secondResult.getData().getData().toString(), true);

        IntermediateEventRepresentation thirdResult = result.get(0);
        assertEquals(TO_PAYLOAD_TYPE, thirdResult.getType().getName());
        assertEquals(PAYLOAD_REVISION, thirdResult.getType().getRevision());
        JSONAssert.assertEquals(expectedThirdEventJson, thirdResult.getData().getData().toString(), true);
    }
}