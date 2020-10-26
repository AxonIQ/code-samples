package io.axoniq.dev.samples;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.RenameMyEntityCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Sara Pellegrini
 */
@RestController
public class CommandController {

    private final CommandGateway commandGateway;

    public CommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping("/entities/{id}")
    public void createMyEntity(@PathVariable("id") String entityId,
                               @RequestParam("name") String name) {
        commandGateway.send(new CreateMyEntityCommand(entityId, name));
    }

    @PatchMapping("/entities/{id}")
    public void renameMyEntity(@PathVariable("id") String entityId,
                               @RequestParam("name") String name) {
        commandGateway.send(new RenameMyEntityCommand(entityId, name));
    }
}


