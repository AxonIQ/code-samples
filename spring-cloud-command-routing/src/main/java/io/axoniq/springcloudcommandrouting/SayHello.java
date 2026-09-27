package io.axoniq.springcloudcommandrouting;

import org.axonframework.messaging.commandhandling.annotation.Command;

/**
 * Asks to greet someone.
 * <p>
 * The {@code routingKey} tells the Spring Cloud connector to route by {@code name}: every command for the same name is
 * handled by the same instance, no matter which instance it was sent from.
 *
 * @param name the name of whom to greet, and the key the command is routed by
 */
@Command(routingKey = "name")
public record SayHello(String name) {

}
