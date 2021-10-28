package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

public class FlightDelayedEvent implements FlightEvent {

    private final FlightId flightId;
    private final Duration delay;

    public FlightDelayedEvent(FlightId flightId, Duration delay) {
        this.flightId = flightId;
        this.delay = delay;
    }

    @Override
    public FlightId getFlightId() {
        return flightId;
    }

    public Duration getDelay() {
        return delay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlightDelayedEvent that = (FlightDelayedEvent) o;
        return Objects.equals(flightId, that.flightId) && Objects.equals(delay, that.delay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightId, delay);
    }

    @Override
    public String toString() {
        return "FlightDelayedEvent{" +
                "flightId=" + flightId +
                ", delay=" + delay +
                '}';
    }
}
