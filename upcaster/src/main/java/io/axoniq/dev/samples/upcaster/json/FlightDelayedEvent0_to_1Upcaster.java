package io.axoniq.dev.samples.upcaster.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.axoniq.dev.samples.api.FlightDelayedEvent;
import org.axonframework.serialization.SimpleSerializedType;
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation;
import org.axonframework.serialization.upcasting.event.SingleEventUpcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Upcaster upcasting the {@code FlightDelayedEvent} from revision {@code 0} to revision {@code 1}.
 * <p>
 * This allows us to adjust the {@link FlightDelayedEvent} implementation to the new format. Part of the adjustment is
 * adding the {@link org.axonframework.serialization.Revision} annotation to the {@code FlightDelayedEvent}. The
 * annotation reflects the new version, defined as revision {@code 1}.
 * <p>
 * The new format of the {@code FlightDelayedEvent} uses a {@link io.axoniq.dev.samples.api.Leg} object to contain the
 * {@code "origin"} and {@code "destination"} fields.
 *
 * @author Yvonne Ceelie
 */
public class FlightDelayedEvent0_to_1Upcaster extends SingleEventUpcaster {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ORIGIN = "origin";
    private static final String DESTINATION = "destination";
    private static final String LEG = "leg";

    private final SimpleSerializedType sourceType =
            new SimpleSerializedType(FlightDelayedEvent.class.getTypeName(), null);
    private final SimpleSerializedType targetType =
            new SimpleSerializedType(FlightDelayedEvent.class.getTypeName(), "1.0");

    @Override
    protected boolean canUpcast(IntermediateEventRepresentation intermediateEventRepresentation) {
        return intermediateEventRepresentation.getType().equals(sourceType);
    }

    @Override
    protected IntermediateEventRepresentation doUpcast(
            IntermediateEventRepresentation intermediateEventRepresentation
    ) {
        logger.info("Upcast event: {}", intermediateEventRepresentation.getType());
        return intermediateEventRepresentation.upcastPayload(targetType, JsonNode.class, this::upcastEvent);
    }

    private JsonNode upcastEvent(JsonNode jsonNode) {
        if (!jsonNode.isObject()) {
            return jsonNode;
        }
        final ObjectNode root = (ObjectNode) jsonNode;
        // Create the leg node and set origin and destination in it
        ObjectNode leg = JsonNodeFactory.instance.objectNode();
        leg.set(ORIGIN, root.get(ORIGIN));
        leg.set(DESTINATION, root.get(DESTINATION));
        root.set(LEG, leg);
        // Remove origin and destination from the root
        root.remove(ORIGIN);
        root.remove(DESTINATION);
        return jsonNode;
    }
}
