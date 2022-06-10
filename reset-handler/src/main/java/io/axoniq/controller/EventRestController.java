package io.axoniq.controller;

import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
    Creates empty events to allow you to see the effects of a reset
 */
@RestController
public class EventRestController {

    private final EventGateway eventGateway;

    public EventRestController(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    @GetMapping("event")
    public void event() {
        eventGateway.publish(new Object());
    }
}
