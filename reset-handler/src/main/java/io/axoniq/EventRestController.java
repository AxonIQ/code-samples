package io.axoniq;

import org.axonframework.messaging.eventhandling.gateway.EventGateway;
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
        // AF5's EventGateway#publish requires an (optional) ProcessingContext as the first argument.
        // There's none available here, so we pass null.
        eventGateway.publish(null, new Object());
    }
}
