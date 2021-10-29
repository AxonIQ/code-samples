package io.axoniq.dev.samples.sequencingpolicy.coreapi;

import java.util.Objects;
import java.util.UUID;

public class FlightId {

    private final String id;

    public FlightId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlightId flightId1 = (FlightId) o;
        return Objects.equals(id, flightId1.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FlightId{" +
                "id=" + id +
                '}';
    }
}
