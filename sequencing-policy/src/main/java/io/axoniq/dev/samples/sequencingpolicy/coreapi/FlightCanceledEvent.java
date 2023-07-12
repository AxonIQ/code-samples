package io.axoniq.dev.samples.sequencingpolicy.coreapi;

public record FlightCanceledEvent(FlightId flightId) implements FlightEvent {

}
