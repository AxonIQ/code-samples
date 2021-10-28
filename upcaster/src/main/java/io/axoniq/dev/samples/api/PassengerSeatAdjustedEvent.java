package io.axoniq.dev.samples.api;

import java.util.Objects;

public class PassengerSeatAdjustedEvent {

    private final String flightId;
    private final String passengerId;
    private final int seatNumber;

    public PassengerSeatAdjustedEvent(String flightId, String passengerId, int seatNumber) {
        this.flightId = flightId;
        this.passengerId = passengerId;
        this.seatNumber = seatNumber;
    }

    public String getFlightId() {
        return flightId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PassengerSeatAdjustedEvent that = (PassengerSeatAdjustedEvent) o;
        return seatNumber == that.seatNumber && Objects.equals(flightId, that.flightId)
                && Objects.equals(passengerId, that.passengerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightId, passengerId, seatNumber);
    }

    @Override
    public String toString() {
        return "PassengerSeatAdjustedEvent{" +
                "flightId='" + flightId + '\'' +
                ", passengerId='" + passengerId + '\'' +
                ", seatNumber=" + seatNumber +
                '}';
    }
}
