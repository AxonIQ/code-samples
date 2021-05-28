package io.axoniq.dev.samples.api;

import java.util.Objects;

public class Leg {

    private final AirportCode origin;
    private final AirportCode destination;

    public Leg(AirportCode origin, AirportCode destination) {
        this.origin = origin;
        this.destination = destination;
    }

    public AirportCode getOrigin() {
        return origin;
    }

    public AirportCode getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Leg leg = (Leg) o;
        return getOrigin() == leg.getOrigin() && getDestination() == leg.getDestination();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrigin(), getDestination());
    }
}
