package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.time.LocalDateTime;
import java.util.Objects;

public class FlightScheduledEvent implements FlightEvent {

    private final FlightId flightId;
    private final String origin;
    private final String destination;
    private final LocalDateTime arrivalTime;

    public FlightScheduledEvent(FlightId flightId, String origin, String destination, LocalDateTime arrivalTime) {
        this.flightId = flightId;
        this.origin = origin;
        this.destination = destination;
        this.arrivalTime = arrivalTime;
    }

    @Override
    public FlightId getFlightId() {
        return flightId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
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
        FlightScheduledEvent that = (FlightScheduledEvent) o;
        return Objects.equals(flightId, that.flightId) && Objects.equals(origin, that.origin)
                && Objects.equals(destination, that.destination) && Objects.equals(arrivalTime, that.arrivalTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightId, origin, destination, arrivalTime);
    }

    @Override
    public String toString() {
        return "FlightScheduledEvent{" +
                "flightId=" + flightId +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", arrivalTime=" + arrivalTime +
                '}';
    }
}
