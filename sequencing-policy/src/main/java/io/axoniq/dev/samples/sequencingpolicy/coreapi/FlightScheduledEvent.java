package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.time.LocalDateTime;

public record FlightScheduledEvent(
        FlightId flightId,
        String origin,
        String destination,
        LocalDateTime arrivalTime
) implements FlightEvent {

}
