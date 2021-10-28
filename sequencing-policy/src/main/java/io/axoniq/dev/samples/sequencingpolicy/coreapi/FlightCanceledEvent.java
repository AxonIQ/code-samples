package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.util.Objects;

public class FlightCanceledEvent implements FlightEvent {

    private final FlightId flightId;

    public FlightCanceledEvent(FlightId flightId) {
        this.flightId = flightId;
    }

    @Override
    public FlightId getFlightId() {
        return flightId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlightCanceledEvent that = (FlightCanceledEvent) o;
        return Objects.equals(flightId, that.flightId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightId);
    }

    @Override
    public String toString() {
        return "FlightCanceledEvent{" +
                "flightId=" + flightId +
                '}';
    }
}
