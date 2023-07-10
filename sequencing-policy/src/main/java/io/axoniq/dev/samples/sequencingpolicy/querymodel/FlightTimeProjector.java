package io.axoniq.dev.samples.sequencingpolicy.querymodel;

import io.axoniq.dev.samples.sequencingpolicy.coreapi.ArrivalTimeChangedEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightCanceledEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightDelayedEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightScheduledEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

/**
 * Simple projector logging the {@link io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightId} and
 * {@link Thread#getId()} for every event it handles.
 * <p>
 * Doing so shows in the logs that the same thread is in charge of all events for a given {@code FlightId}. This further
 * shows that the {@link org.axonframework.eventhandling.async.SequencingPolicy} based on the {@code FlightId} is in
 * effect.
 */
@Component
@ProcessingGroup("flight-time")
public class FlightTimeProjector {

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
