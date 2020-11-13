package io.axoniq.dev.samples.command.interceptor;

import java.util.List;
import java.util.function.BiFunction;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.stereotype.Component;

import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.command.persistence.EmailRepository;

/**
 * Intercepts the create account command message and throws IllegalStateException when already account aggregate with
 * email address already exists. Links to "Validation through a Dispatch Interceptor' section in in this [set based
 * validation blog](https://axoniq.io/blog-overview/set-based-validation)
 *
 * @author Yvonne Ceelie
 */
@Component
public class AccountCreationDispatchInterceptor implements MessageDispatchInterceptor<CommandMessage<?>> {

    private final EmailRepository emailRepository;

    public AccountCreationDispatchInterceptor(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(List<? extends CommandMessage<?>> list) {
        return (i, m) -> {
            if (CreateAccountCommand.class.equals(m.getPayloadType())) {
                final CreateAccountCommand createAccountCommand = (CreateAccountCommand) m.getPayload();
                if (emailRepository.existsById(createAccountCommand.getEmailAddress())) {
                    throw new IllegalStateException(String.format("Account with email address %s already exists",
                                                                  createAccountCommand.getEmailAddress()));
                }
            }
            return m;
        };
    }
}
