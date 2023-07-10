package io.axoniq.dev.samples.query.eventhandler;

import io.axoniq.dev.samples.api.commands.RemoveUserCommand;
import io.axoniq.dev.samples.api.events.EmailAddressRemovedEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserEventHandler {

    private final CommandGateway commandGateway;

    public UserEventHandler(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @EventHandler
    public void handle(EmailAddressRemovedEvent event) {
        commandGateway.send(new RemoveUserCommand(event.userId()));
    }
}
