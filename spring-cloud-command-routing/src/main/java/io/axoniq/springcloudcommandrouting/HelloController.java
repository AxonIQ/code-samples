package io.axoniq.springcloudcommandrouting;

import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Sends a {@link SayHello} command for every request, and replies with whatever the handling instance answered.
 */
@RestController
class HelloController {

    private final CommandGateway commandGateway;

    HelloController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @GetMapping("/hello/{name}")
    CompletableFuture<String> hello(@PathVariable String name) {
        return commandGateway.send(new SayHello(name), String.class);
    }
}
