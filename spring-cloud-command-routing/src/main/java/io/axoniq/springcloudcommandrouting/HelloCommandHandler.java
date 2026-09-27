package io.axoniq.springcloudcommandrouting;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.stereotype.Component;

/**
 * Answers {@link SayHello} with a greeting naming the instance that handled it.
 * <p>
 * Every instance runs this handler. Which one handles a given command is decided by the connector's routing, so the
 * instance id in the reply shows where the command went.
 */
@Component
class HelloCommandHandler {

    private final String instanceId;

    HelloCommandHandler(Registration registration) {
        this.instanceId = registration.getInstanceId();
    }

    @CommandHandler
    String handle(SayHello command) {
        return "Hello " + command.name() + ", from " + instanceId;
    }
}
