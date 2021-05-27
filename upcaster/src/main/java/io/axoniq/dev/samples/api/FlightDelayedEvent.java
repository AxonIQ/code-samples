package io.axoniq.dev.samples.api;

import org.axonframework.serialization.Revision;

import java.time.LocalDateTime;
import java.util.Objects;

@Revision("1.0")
public class FlightDelayedEvent {

    private final String flightId;
    private final Leg leg;
    private final LocalDateTime arrivalTime;

    public FlightDelayedEvent(String flightId, Leg leg, LocalDateTime arrivalTime) {
        this.flightId = flightId;
        this.leg = leg;
        this.arrivalTime = arrivalTime;
    }

    public String getFlightId() {
        return flightId;
    }

    public Leg getLeg() {
        return leg;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
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
        return Objects.equals(getFlightId(), that.getFlightId()) && Objects.equals(getLeg(),
                                                                                   that.getLeg())
                && Objects.equals(getArrivalTime(), that.getArrivalTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFlightId(), getLeg(), getArrivalTime());
    }
}
