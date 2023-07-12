package io.axoniq.dev.samples.api;

public record PassengerSeatAdjustedEvent(
        String flightId,
        String passengerId,
        int seatNumber
) {

}
