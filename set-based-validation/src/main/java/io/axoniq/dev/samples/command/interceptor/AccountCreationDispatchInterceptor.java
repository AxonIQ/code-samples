package io.axoniq.dev.samples.command.interceptor;

import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.validator.EmailUniquenessValidator;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Intercepts the create account command message and throws IllegalStateException when already account aggregate with
 * email address already exists. Links to "Validation through a Dispatch Interceptor' section in in this [set based
 * validation blog](https://axoniq.io/blog-overview/set-based-validation)
 *
 * @author Yvonne Ceelie
 */
@Component
public class AccountCreationDispatchInterceptor implements MessageDispatchInterceptor<CommandMessage<?>> {

    private final EmailUniquenessValidator emailUniquenessValidator;

    public AccountCreationDispatchInterceptor(EmailUniquenessValidator emailUniquenessValidator) {
        this.emailUniquenessValidator = emailUniquenessValidator;
    }

    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(List<? extends CommandMessage<?>> list) {
        return (i, m) -> {
            if (CreateAccountCommand.class.equals(m.getPayloadType())) {
                final CreateAccountCommand createAccountCommand = (CreateAccountCommand) m.getPayload();
                emailUniquenessValidator.validateEmailAddress(createAccountCommand.getEmailAddress());
            }
            return m;
        };
    }
}
