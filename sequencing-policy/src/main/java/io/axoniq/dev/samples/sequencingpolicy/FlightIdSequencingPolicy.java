package io.axoniq.dev.samples.sequencingpolicy;

import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightEvent;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.async.SequencingPolicy;

/**
 * Custom implementation of the {@link SequencingPolicy} that returns the {@link io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightId}
 * from {@link FlightEvent} implementations.
 * <p>
 * This ensures a given segment of the event stream will handle all the {@code FlightEvents} with the same {@code
 * FlightId} in the correct order. If the event being handled isn't an implementation of {@code FlightEvent}, {@code
 * null} is returned.
 * <p>
 * Key in simplifying this implementation, is the {@code FlightEvent} interface that is implemented by all flight
 * related events. Without this, every implementation should be validated separately.
 *
 * @author Steven van Beelen
 */
public class FlightIdSequencingPolicy implements SequencingPolicy<EventMessage<?>> {

    @Override
    public Object getSequenceIdentifierFor(EventMessage<?> event) {
        return FlightEvent.class.isAssignableFrom(event.getPayloadType())
                ? ((FlightEvent) event.getPayload()).getFlightId()
                : null; // when returning null, Axon will default to EventMessage#getIdentifier
    }
}
