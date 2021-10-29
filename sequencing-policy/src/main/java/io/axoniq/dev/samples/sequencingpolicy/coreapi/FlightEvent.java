package io.axoniq.dev.samples.sequencingpolicy.coreapi;

/**
 * Common base event to implement by every flight event.
 * <p>
 * This common base interface ensures Axon can use logic based on the {@link FlightId} without knowing all flight event
 * implementations.
 *
 * @author Steven van Beelen
 */
public interface FlightEvent {

    FlightId getFlightId();
}
