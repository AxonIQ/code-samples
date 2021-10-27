package io.axoniq.dev.samples.upcaster.json;

import org.axonframework.config.Configurer;
import org.axonframework.serialization.upcasting.event.EventUpcasterChain;

import java.util.function.Function;

/**
 * Utility class constructing the {@link EventUpcasterChain} to configure on the {@link
 * org.axonframework.eventsourcing.eventstore.EventStore}.
 * <p>
 * In a Spring Boot environment exposing an {@code EventUpcasterChain} bean is sufficient for the framework to pick it
 * up correctly. To that end we can use the {@link #buildEventUpcasterChain()} method.
 * <p>
 * When using Axon's {@link org.axonframework.config.Configurer} directly, you should configure all upcasters separately
 * by invoking the {@link org.axonframework.config.Configurer#registerEventUpcaster(Function)} method. The {@link
 * #configureUpcasters(Configurer)} shows how we should implement this.
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

    /**
     * Configures all the upcasters of this application with the given {@code configurer}.
     * <p>
     * Can be utilized to configure upcasters if the application code uses Axon's {@link Configurer} directly.
     *
     * @param configurer the {@link Configurer} to register all upcasters with
     */
    public static void configureUpcasters(Configurer configurer) {
        configurer.registerEventUpcaster(config -> new FlightDelayedEvent0_to_1Upcaster());
    }

    private EventUpcasterChainFactory() {
        // Utility class
    }
}
