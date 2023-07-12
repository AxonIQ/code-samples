package io.axoniq.dev.samples.api;

/**
 * Event containing an update to the stream.
 *
 * @author Steven van Beelen
 */
public record StreamUpdatedEvent(
        String update
) {

}
