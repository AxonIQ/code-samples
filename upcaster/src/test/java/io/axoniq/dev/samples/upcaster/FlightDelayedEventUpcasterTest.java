package io.axoniq.dev.samples.upcaster;

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FlightDelayedEventUpcasterTest {

    public static final String PAYLOAD_TYPE = "io.axoniq.dev.samples.api.FlightDelayedEvent";
    public static final String INCORRECT_PAYLOAD_TYPE = "io.axoniq.dev.samples.api.FlightCreatedEvent";

    public static final String PRE_UPCASTER_REVISION = null;
    public static final String UPCASTED_REVISION = "1.0";
    public static final String FLIGHT_CREATED_EVENT_REVISION = "3.0";

    public static final String FLIGHT_DELAYED_EVENT_REV_NULL_JSON_FILE_NAME = "/FlightDelayedEventRev_null.json";
    public static final String FLIGHT_DELAYED_EVENT_REV_1_JSON_FILE_NAME = "/FlightDelayedEventRev_1.json";
    public static final String PAYLOAD_OF_FLIGHT_CREATED_EVENT = "Payload of FlightCreatedEvent";

    public String flightDelayedEventVersionNullPayload;
    public String flightDelayedEventVersion1Payload;

    private final FlightDelayedEventUpcaster testSubject = new FlightDelayedEventUpcaster();

    public static final String FLIGHT_ID = "KL123";

    private EventData<?> flightDelayedEventWithRevisionNull;
    private EventData<?> flightDelayedEventWithRevision1;
    private EventData<?> flightDelayedEventWithIncorrectPayloadType;

    private final Serializer serializer = JacksonSerializer.defaultSerializer();


    @BeforeEach
    void setUp() {
        final InputStream flightDelayedEventVersionNullPayloadStream = this.getClass().getResourceAsStream(
                FLIGHT_DELAYED_EVENT_REV_NULL_JSON_FILE_NAME);
        flightDelayedEventVersionNullPayload = inputStreamToString(flightDelayedEventVersionNullPayloadStream);

        final InputStream flightDelayedEventVersion1PayloadStream = this.getClass().getResourceAsStream(
                FLIGHT_DELAYED_EVENT_REV_1_JSON_FILE_NAME);
        flightDelayedEventVersion1Payload = inputStreamToString(flightDelayedEventVersion1PayloadStream);

        flightDelayedEventWithRevisionNull = new TestEventEntry(PAYLOAD_TYPE,
                                                                PRE_UPCASTER_REVISION,
                                                                flightDelayedEventVersionNullPayload);
        flightDelayedEventWithRevision1 = new TestEventEntry(PAYLOAD_TYPE,
                                                             UPCASTED_REVISION,
                                                             flightDelayedEventVersion1Payload);
        flightDelayedEventWithIncorrectPayloadType = new TestEventEntry(INCORRECT_PAYLOAD_TYPE,
                                                                        FLIGHT_CREATED_EVENT_REVISION,
                                                                        PAYLOAD_OF_FLIGHT_CREATED_EVENT);
    }

    @Test
    void testCanUpcastReturnsTrueForMatchingPayloadTypeAndRevision() {
        IntermediateEventRepresentation testRepresentation = new InitialEventRepresentation(
                flightDelayedEventWithRevisionNull,
                serializer);

        assertTrue(testSubject.canUpcast(testRepresentation));
    }

    @Test
    void testCanUpcastReturnsFalseForIncorrectPayloadType() {
        IntermediateEventRepresentation testRepresentation = new InitialEventRepresentation(
                flightDelayedEventWithIncorrectPayloadType,
                serializer);

        assertFalse(testSubject.canUpcast(testRepresentation));
    }

    @Test
    void testCanUpcastReturnsFalseForIncorrectRevision() {
        IntermediateEventRepresentation testRepresentation = new InitialEventRepresentation(
                flightDelayedEventWithRevision1,
                serializer);

        assertFalse(testSubject.canUpcast(testRepresentation));
    }

    @Test
    void testDoUpcast() throws JSONException {
        InitialEventRepresentation testRepresentation = new InitialEventRepresentation(
                flightDelayedEventWithRevisionNull,
                serializer);

        IntermediateEventRepresentation result = testSubject.doUpcast(testRepresentation);
        SerializedType resultType = result.getType();
        assertEquals(PAYLOAD_TYPE, resultType.getName());
        assertEquals(UPCASTED_REVISION, resultType.getRevision());
        JSONAssert.assertEquals(flightDelayedEventVersion1Payload,
                                result.getData().getData().toString(),
                                true);
    }

    private String inputStreamToString(InputStream inputStream) {
        return new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    private static class TestEventEntry extends AbstractEventEntry<String> {

        public TestEventEntry(String payloadType, String payloadRevision, String payload) {
            super(FLIGHT_ID, "timestamp", payloadType, payloadRevision, payload, "metaData");
        }
    }
}
