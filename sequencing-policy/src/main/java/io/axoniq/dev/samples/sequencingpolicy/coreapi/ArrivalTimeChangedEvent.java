package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.time.Instant;

public class ArrivalTimeChangedEvent implements FlightEvent {

    private final FlightId flightId;
    private final Instant newArrivalTime;

    public ArrivalTimeChangedEvent(FlightId flightId, Instant newArrivalTime) {
        this.flightId = flightId;
        this.newArrivalTime = newArrivalTime;
    }

    @Override
    public FlightId getFlightId() {
        return flightId;
    }

    public Instant getNewArrivalTime() {
        return newArrivalTime;
    }

    @Override
    public String toString() {
        return "ArrivalTimeChangedEvent{" +
                "flightId=" + flightId +
                ", newArrivalTime=" + newArrivalTime +
                '}';
    }
}
