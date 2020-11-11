package io.axoniq.dev.samples.command.handler;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.RequestEmailChangeCommand;
import io.axoniq.dev.samples.command.persistence.EmailRepository;

/**
 * Command handling component that validates if the email address already exists. Links to the "Validation in an
 * External Command Handler" section in <a href="https://axoniq.io/blog-overview/set-based-validation">Set based
 * consistency validation with Axon</a>.
 *
 * @author Yvonne Ceelie
 */
@Component
public class AccountCommandHandler {

    @CommandHandler
    public void handle(RequestEmailChangeCommand command, CommandGateway commandGateway,
                       EmailRepository emailRepository) {
        if (emailRepository.existsById(command.getUpdatedEmailAddress())) {
            throw new IllegalStateException(String.format("Account with email address %s already exists",
                                                          command.getUpdatedEmailAddress()));
        }
        commandGateway.send(new ChangeEmailAddressCommand(command.getAccountId(), command.getUpdatedEmailAddress()));
    }
}
