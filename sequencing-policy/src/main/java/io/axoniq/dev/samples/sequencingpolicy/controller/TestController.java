package io.axoniq.dev.samples.sequencingpolicy.controller;

import io.axoniq.dev.samples.sequencingpolicy.coreapi.ArrivalTimeChangedEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightCanceledEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightDelayedEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightId;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightScheduledEvent;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

@Controller
@RequestMapping("test")
public class TestController {

    private static final String[] DESTINATIONS = new String[]{
            "HEL", "CDG", "ORY", "CGN", "MUC", "BUD", "DUB", "LIN", "MXP", "SKP", "AMS", "EIN", "WRO", "BEG", "BCN",
            "MAD"
    };
    private static final Random RANDOM = new Random();

    private final EventGateway eventGateway;

    public TestController(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    @PostMapping("bulk/{amount}")
    public ResponseEntity<Void> bulkEvents(@PathVariable Integer amount) {
        IntStream.range(0, amount)
                 .forEach(i -> {
                     FlightId flightId = new FlightId(UUID.randomUUID().toString());
                     LocalDateTime scheduledArrival = LocalDateTime.now();
                     String origin = fetchDestination();
                     String destination = fetchDestination();
                     eventGateway.publish(
                             new FlightScheduledEvent(flightId, origin, destination, scheduledArrival),
                             new FlightDelayedEvent(flightId, Duration.ofHours(1)),
                             new ArrivalTimeChangedEvent(
                                     flightId, scheduledArrival.plus(1, ChronoUnit.HOURS).toInstant(ZoneOffset.UTC)
                             ),
                             new FlightDelayedEvent(flightId, Duration.ofHours(10)),
                             new FlightCanceledEvent(flightId)
                     );
                 });

        return ResponseEntity.ok().build();
    }

    private static String fetchDestination() {
        return DESTINATIONS[RANDOM.nextInt(DESTINATIONS.length)];
    }
}
