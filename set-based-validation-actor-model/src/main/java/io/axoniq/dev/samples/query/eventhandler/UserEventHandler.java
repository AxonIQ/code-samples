package io.axoniq.dev.samples.query.eventhandler;

import io.axoniq.dev.samples.api.commands.RemoveUser;
import io.axoniq.dev.samples.api.events.EmailAddressRemoved;
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
    public void handle(EmailAddressRemoved event){
        commandGateway.send(new RemoveUser(event.getUserId()));
    }

}
