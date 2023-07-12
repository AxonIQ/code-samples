package io.axoniq;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class MyFakeProjection {

    @EventHandler
    public void on(Object object) {
        System.out.println("Got an object");
    }
}
