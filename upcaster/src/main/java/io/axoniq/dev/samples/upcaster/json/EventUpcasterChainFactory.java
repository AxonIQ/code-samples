package io.axoniq.dev.samples.upcaster.json;

import org.axonframework.serialization.upcasting.event.EventUpcasterChain;

/**
 * Utility class constructing the {@link EventUpcasterChain} to configure on the {@link
 * org.axonframework.eventsourcing.eventstore.EventStore}.
 * <p>
 * In a Spring Boot environment exposing an {@code EventUpcasterChain} bean is sufficient for the framework to pick it
 * up correctly. To that end we can use the {@link #buildEventUpcasterChain()} method.
 *
 * @author Yvonne Ceelie
 */
public abstract class EventUpcasterChainFactory {

    /**
     * Constructs an {@link EventUpcasterChain} combining all the upcasters of this application.
     * <p>
     * Can be used to expose an {@code EventUpcasterChain} bean in a Spring environment.
     *
     * @return an {@link EventUpcasterChain}
     */
    public static EventUpcasterChain buildEventUpcasterChain() {
        return new EventUpcasterChain(
                new FlightDelayedEvent0_to_1Upcaster()
        );
    }

    private EventUpcasterChainFactory() {
        // Utility class
    }
}
