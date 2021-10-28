package io.axoniq.dev.samples.upcaster.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.axonframework.serialization.SimpleSerializedType;
import org.axonframework.serialization.upcasting.event.EventMultiUpcaster;
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An upcaster implementation that, instead of returning a single entry, returns a {@link Stream} of {@link
 * IntermediateEventRepresentation}s for the {@link io.axoniq.dev.samples.api.PassengerSeatsAdjustedEvent}.
 * <p>
 * This {@link EventMultiUpcaster} implementation retrieves the {@code "passengerSeats"} collection from the deprecated
 * event. Doing os it is able to upcast the single event to the right amount of {@link
 * io.axoniq.dev.samples.api.PassengerSeatAdjustedEvent}s.
 * <p>
 * Note that the original event is removed from the result, as it is not part of the returned {@code Stream} by the
 * {@link #doUpcast(IntermediateEventRepresentation)} implementation.
 *
 * @author Steven van Beelen
 */
public class PassengerSeatsToPassengerSeatAdjustedEventUpcaster extends EventMultiUpcaster {

    private static final SimpleSerializedType FROM =
            new SimpleSerializedType("io.axoniq.dev.samples.api.PassengerSeatsAdjustedEvent", null);

    private static final SimpleSerializedType TO =
            new SimpleSerializedType("io.axoniq.dev.samples.api.PassengerSeatAdjustedEvent", null);

    private static final String PASSENGER_SEATS_FIELD = "passengerSeats";
    private static final String PASSENGER_ID_FIELD = "passengerId";
    private static final String SEAT_NUMBER_FIELD = "seatNumber";

    @Override
    protected boolean canUpcast(IntermediateEventRepresentation intermediateRepresentation) {
        return intermediateRepresentation.getType().equals(FROM);
    }

    @Override
    protected Stream<IntermediateEventRepresentation> doUpcast(IntermediateEventRepresentation intermediateRep) {
        JsonNode eventJsonNode = intermediateRep.getData(JsonNode.class).getData();
        if (!eventJsonNode.isObject()) {
            return Stream.of(intermediateRep);
        }

        ObjectNode eventData = (ObjectNode) eventJsonNode;
        Spliterator<Map.Entry<String, JsonNode>> passengerSpliterator = Spliterators.spliteratorUnknownSize(
                eventData.get(PASSENGER_SEATS_FIELD).fields(), Spliterator.ORDERED
        );

        return StreamSupport.stream(passengerSpliterator, false)
                            .map(passengerEntry -> intermediateRep.upcastPayload(
                                    TO, JsonNode.class, eventJson -> upcastToSeatAdjustedJson(
                                            eventJson, passengerEntry.getKey(), passengerEntry.getValue()
                                    )
                            ));
    }

    private JsonNode upcastToSeatAdjustedJson(JsonNode eventJson, String passengerId, JsonNode passengerSeat) {
        if (!eventJson.isObject()) {
            return eventJson;
        }
        ObjectNode eventData = (ObjectNode) eventJson;

        // Remove the map of passengerId to seatNumber
        eventData.remove(PASSENGER_SEATS_FIELD);

        // And replace for a separate passengerId and seatNumber field
        eventData.set(PASSENGER_ID_FIELD, new TextNode(passengerId));
        eventData.set(SEAT_NUMBER_FIELD, passengerSeat);

        return eventData;
    }
}
