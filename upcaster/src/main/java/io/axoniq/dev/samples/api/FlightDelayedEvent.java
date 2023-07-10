package io.axoniq.dev.samples.api;

import org.axonframework.serialization.Revision;

import java.time.LocalDateTime;
import java.util.Objects;

@Revision("1.0")
public record FlightDelayedEvent(
        String flightId,
        Leg leg,
        LocalDateTime arrivalTime
) {

}
