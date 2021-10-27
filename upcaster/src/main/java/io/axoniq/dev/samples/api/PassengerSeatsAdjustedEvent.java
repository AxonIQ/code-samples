package io.axoniq.dev.samples.api;

import java.util.Map;
import java.util.Objects;

/**
 * An old event of the flight domain that's now been deprecated.
 * <p>
 * This event contained a complete list of all the passenger seat adjustments of a flight in one go. As time passed, the
 * application developers noticed they required an event for every separate change. Hence, they introduced that {@link
 * PassengerSeatAdjustedEvent} and added a one-to-many upcaster to make this adjustment.
 *
 * @author Steven van Beelen
 * @see io.axoniq.dev.samples.upcaster.json.PassengerSeatsToPassengerSeatAdjustedEventUpcaster
 * @deprecated in favor of singular {@link PassengerSeatAdjustedEvent}s
 */
@Deprecated
public class PassengerSeatsAdjustedEvent {

    private final String flightId;
    private final Map<String, Integer> passengerSeats;

    public PassengerSeatsAdjustedEvent(String flightId, Map<String, Integer> passengerSeats) {
        this.flightId = flightId;
        this.passengerSeats = passengerSeats;
    }

    public String getFlightId() {
        return flightId;
    }

    public Map<String, Integer> getPassengerSeats() {
        return passengerSeats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PassengerSeatsAdjustedEvent that = (PassengerSeatsAdjustedEvent) o;
        return Objects.equals(flightId, that.flightId) && Objects.equals(passengerSeats, that.passengerSeats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightId, passengerSeats);
    }

    @Override
    public String toString() {
        return "PassengerSeatsAdjustedEvent{" +
                "flightId='" + flightId + '\'' +
                ", passengerSeats=" + passengerSeats +
                '}';
    }
}
