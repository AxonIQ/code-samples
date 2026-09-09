package io.axoniq;

import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class MyFakeProjection {

    @EventHandler
    public void on(Object object) {
        System.out.println("Got an object");
    }
}
