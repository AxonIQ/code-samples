package io.axoniq.dev.samples.rest;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.RenameMyEntityCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/entity")
class CommandController {

    private final CommandGateway commandGateway;

    public CommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping("/{id}")
    public CompletableFuture<Void> createMyEntity(@PathVariable("id") String entityId,
                                                  @RequestParam("name") String name) {
        return commandGateway.send(new CreateMyEntityCommand(entityId, name));
    }

    @PatchMapping("/{id}")
    public CompletableFuture<Void> renameMyEntity(@PathVariable("id") String entityId,
                                                  @RequestParam("name") String name) {
        return commandGateway.send(new RenameMyEntityCommand(entityId, name));
    }
}


