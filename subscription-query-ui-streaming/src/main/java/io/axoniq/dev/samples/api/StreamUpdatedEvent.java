package io.axoniq.dev.samples.api;

import java.util.Objects;

/**
 * Event containing an update to the stream.
 *
 * @author Steven van Beelen
 */
public class StreamUpdatedEvent {

    private final String update;

    public StreamUpdatedEvent(String update) {
        this.update = update;
    }

    public String getUpdate() {
        return update;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StreamUpdatedEvent that = (StreamUpdatedEvent) o;
        return Objects.equals(update, that.update);
    }

    @Override
    public int hashCode() {
        return Objects.hash(update);
    }

    @Override
    public String toString() {
        return "StreamUpdatedEvent{" +
                "update='" + update + '\'' +
                '}';
    }
}
