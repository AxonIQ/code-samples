package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.time.Instant;

public record ArrivalTimeChangedEvent(
        FlightId flightId,
        Instant newArrivalTime
) implements FlightEvent {

}
