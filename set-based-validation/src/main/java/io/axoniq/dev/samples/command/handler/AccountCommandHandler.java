package io.axoniq.dev.samples.command.handler;

import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.RequestEmailChangeCommand;
import io.axoniq.dev.samples.validator.EmailUniquenessValidator;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

/**
 * Command handling component that validates if the email address already exists. Links to the `Validation in an
 * External Command Handler` section in this [set based validation blog](https://axoniq.io/blog-overview/set-based-validation)
 *
 * @author Yvonne Ceelie
 */
@Component
public class AccountCommandHandler {

    @CommandHandler
    public void handle(RequestEmailChangeCommand command, CommandGateway commandGateway,
                       EmailUniquenessValidator emailUniquenessValidator) {
        emailUniquenessValidator.validateEmailAddress(command.getUpdatedEmailAddress());
        commandGateway.send(new ChangeEmailAddressCommand(command.getAccountId(), command.getUpdatedEmailAddress()));
    }
}
