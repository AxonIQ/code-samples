package io.axoniq.dev.samples.api;

import java.util.Map;

/**
 * An old event of the flight domain that's now been deprecated.
 * <p>
 * This event contained a complete list of all the passenger seat adjustments of a flight in one go. As time passed, the
 * application developers noticed they required an event for every separate change. Hence, they introduced that
 * {@link PassengerSeatAdjustedEvent} and added a one-to-many upcaster to make this adjustment.
 *
 * @author Steven van Beelen
 * @see io.axoniq.dev.samples.upcaster.json.PassengerSeatsToPassengerSeatAdjustedEventUpcaster
 * @deprecated in favor of singular {@link PassengerSeatAdjustedEvent}s
 */
@Deprecated
public record PassengerSeatsAdjustedEvent(
        String flightId,
        Map<String, Integer> passengerSeats
) {

}
