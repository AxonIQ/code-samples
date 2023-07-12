package io.axoniq.dev.samples.command.handler;

import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.RequestEmailChangeCommand;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

/**
 * Command handling component that validates if the email address already exists. Links to the `Validation in an
 * External Command Handler` section in this <a href="https://axoniq.io/blog-overview/set-based-validation">set based
 * validation blog</a>.
 *
 * @author Yvonne Ceelie
 */
@Component
public class AccountCommandHandler {

    @CommandHandler
    public void handle(RequestEmailChangeCommand command, CommandGateway commandGateway,
                       EmailRepository emailRepository) {
        if (emailRepository.existsById(command.updatedEmailAddress())) {
            throw new IllegalStateException(String.format("Account with email address %s already exists",
                                                          command.updatedEmailAddress()));
        }
        commandGateway.send(new ChangeEmailAddressCommand(command.accountId(), command.updatedEmailAddress()));
    }
}
