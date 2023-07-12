package io.axoniq;

import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
