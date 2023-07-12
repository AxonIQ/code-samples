package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.time.Duration;

public record FlightDelayedEvent(
        FlightId flightId,
        Duration delay
) implements FlightEvent {

}
