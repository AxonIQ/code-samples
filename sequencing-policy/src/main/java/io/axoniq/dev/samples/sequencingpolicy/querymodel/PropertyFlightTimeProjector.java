package io.axoniq.dev.samples.sequencingpolicy.querymodel;

import io.axoniq.dev.samples.sequencingpolicy.coreapi.ArrivalTimeChangedEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightCanceledEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightDelayedEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightScheduledEvent;
import org.axonframework.messaging.core.annotation.Namespace;
import org.axonframework.messaging.core.annotation.SequencingPolicy;
import org.axonframework.messaging.core.sequencing.PropertySequencingPolicy;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

/**
 * Identical to {@link FlightTimeProjector}, but configured with the built-in {@link PropertySequencingPolicy} instead
 * of the custom {@link io.axoniq.dev.samples.sequencingpolicy.FlightIdSequencingPolicy}.
 * <p>
 * Added to show that the {@code PropertySequencingPolicy} works identically to the custom
 * {@code FlightIdSequencingPolicy}: since every flight event exposes a {@code flightId} property (through the common
 * {@link io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightEvent} interface), the {@code SequencingPolicy}
 * annotation's constructor-parameter resolution can inject each handled event's concrete payload type automatically,
 * requiring only the property name to be provided.
 * <p>
 * This projector is only registered when the {@code policy} property equals {@code property}.
 */
@Component
@Namespace("flight-time")
@ConditionalOnProperty(value = "policy", havingValue = "property")
@SequencingPolicy(type = PropertySequencingPolicy.class, parameters = {"flightId"})
public class PropertyFlightTimeProjector {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @EventHandler
    public void on(FlightScheduledEvent event) {
        logger.info("Thread[{}] handling FlightScheduledEvent with Flight Id [{}]",
                    Thread.currentThread().getId(), event.flightId());
    }

    @EventHandler
    public void on(FlightDelayedEvent event) {
        logger.info("Thread[{}] handling FlightDelayedEvent with Flight Id [{}]",
                    Thread.currentThread().getId(), event.flightId());
    }

    @EventHandler
    public void on(FlightCanceledEvent event) {
        logger.info("Thread[{}] handling FlightCanceledEvent with Flight Id [{}]",
                    Thread.currentThread().getId(), event.flightId());
    }

    @EventHandler
    public void on(ArrivalTimeChangedEvent event) {
        logger.info("Thread[{}] handling ArrivalTimeChangedEvent with Flight Id [{}]",
                    Thread.currentThread().getId(), event.flightId());
    }
}
