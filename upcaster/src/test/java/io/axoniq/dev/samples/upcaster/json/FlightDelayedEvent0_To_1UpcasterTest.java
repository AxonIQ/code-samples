package io.axoniq.dev.samples.upcaster.json;

import org.axonframework.eventhandling.AbstractEventEntry;
import org.axonframework.eventhandling.EventData;
import org.axonframework.serialization.SerializedType;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.upcasting.event.InitialEventRepresentation;
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.skyscreamer.jsonassert.JSONAssert;

import java.time.Instant;
import java.util.UUID;

import static io.axoniq.dev.samples.upcaster.json.UpcasterTestingUtils.extractFileContentsToString;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("NewClassNamingConvention")
class FlightDelayedEvent0_To_1UpcasterTest {

    private static final String PAYLOAD_TYPE = "io.axoniq.dev.samples.api.FlightDelayedEvent";
    private static final String INCORRECT_PAYLOAD_TYPE = "io.axoniq.dev.samples.api.FlightCreatedEvent";

    private static final String PRE_UPCASTER_REVISION = null;
    private static final String UPCASTED_REVISION = "1.0";
    private static final String FLIGHT_CREATED_EVENT_REVISION = "3.0";

    private static final String FLIGHT_DELAYED_EVENT_REV_NULL_JSON_FILE_NAME = "/FlightDelayedEventRev_null.json";
    private static final String FLIGHT_DELAYED_EVENT_REV_1_JSON_FILE_NAME = "/FlightDelayedEventRev_1.json";
    private static final String PAYLOAD_OF_FLIGHT_CREATED_EVENT = "Payload of FlightCreatedEvent";

    private String flightDelayedEventVersion1Payload;

    private final FlightDelayedEvent0_to_1Upcaster testSubject = new FlightDelayedEvent0_to_1Upcaster();

    private EventData<?> flightDelayedEventWithRevisionNull;
    private EventData<?> flightDelayedEventWithRevision1;
    private EventData<?> flightDelayedEventWithIncorrectPayloadType;

    private final Serializer serializer = JacksonSerializer.defaultSerializer();

    @BeforeEach
    void setUp() {
        String flightDelayedEventVersionNullPayload =
                extractFileContentsToString(FLIGHT_DELAYED_EVENT_REV_NULL_JSON_FILE_NAME);
        flightDelayedEventVersion1Payload = extractFileContentsToString(FLIGHT_DELAYED_EVENT_REV_1_JSON_FILE_NAME);

        flightDelayedEventWithRevisionNull =
                new TestEventEntry(PAYLOAD_TYPE, PRE_UPCASTER_REVISION, flightDelayedEventVersionNullPayload);
        flightDelayedEventWithRevision1 =
                new TestEventEntry(PAYLOAD_TYPE, UPCASTED_REVISION, flightDelayedEventVersion1Payload);
        flightDelayedEventWithIncorrectPayloadType = new TestEventEntry(
                INCORRECT_PAYLOAD_TYPE, FLIGHT_CREATED_EVENT_REVISION, PAYLOAD_OF_FLIGHT_CREATED_EVENT
        );
    }

    @Test
    void testCanUpcastReturnsTrueForMatchingPayloadTypeAndRevision() {
        IntermediateEventRepresentation testRepresentation =
                new InitialEventRepresentation(flightDelayedEventWithRevisionNull, serializer);

        assertTrue(testSubject.canUpcast(testRepresentation));
    }

    @Test
    void testCanUpcastReturnsFalseForIncorrectPayloadType() {
        IntermediateEventRepresentation testRepresentation =
                new InitialEventRepresentation(flightDelayedEventWithIncorrectPayloadType, serializer);

        assertFalse(testSubject.canUpcast(testRepresentation));
    }

    @Test
    void testCanUpcastReturnsFalseForIncorrectRevision() {
        IntermediateEventRepresentation testRepresentation =
                new InitialEventRepresentation(flightDelayedEventWithRevision1, serializer);

        assertFalse(testSubject.canUpcast(testRepresentation));
    }

    @Test
    void testDoUpcast() throws JSONException {
        InitialEventRepresentation testRepresentation =
                new InitialEventRepresentation(flightDelayedEventWithRevisionNull, serializer);

        IntermediateEventRepresentation result = testSubject.doUpcast(testRepresentation);
        SerializedType resultType = result.getType();
        assertEquals(PAYLOAD_TYPE, resultType.getName());
        assertEquals(UPCASTED_REVISION, resultType.getRevision());
        JSONAssert.assertEquals(flightDelayedEventVersion1Payload, result.getData().getData().toString(), true);
    }

    private static class TestEventEntry extends AbstractEventEntry<String> {

        public TestEventEntry(String payloadType, String payloadRevision, String payload) {
            super(UUID.randomUUID().toString(), Instant.now(), payloadType, payloadRevision, payload, "metaData");
        }
    }
}
