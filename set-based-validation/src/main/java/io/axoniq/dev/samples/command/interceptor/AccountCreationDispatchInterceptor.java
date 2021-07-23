package io.axoniq.dev.samples.command.interceptor;

import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.EventTrackerStatus;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
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

    private final EmailRepository emailRepository;

    private final Configuration configuration;

    public AccountCreationDispatchInterceptor(EmailRepository emailRepository, Configuration configuration) {
        this.emailRepository = emailRepository;
        this.configuration = configuration;
    }

    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(List<? extends CommandMessage<?>> list) {
        return (i, m) -> {
            Optional<StreamingEventProcessor> emailEntityProcessor = configuration
                    .eventProcessingConfiguration().eventProcessorByProcessingGroup("emailEntityProcessor");
            emailEntityProcessor.ifPresentOrElse(this::throwExceptionWhenReplaying,
                                                 this::throwExceptionWhenProcessorIsNotConfigured);
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

    private void throwExceptionWhenReplaying(StreamingEventProcessor eventProcessor) {
        if (eventProcessor.isReplaying() || !isCaughtUp(eventProcessor)) {
            throw new IllegalStateException("Email event processor is not up to date");
        }
    }

    private void throwExceptionWhenProcessorIsNotConfigured() {
        throw new IllegalStateException("Email event processor is not configured");
    }

    private boolean isCaughtUp(StreamingEventProcessor eventProcessor) {
        return eventProcessor.processingStatus().values().stream().allMatch(EventTrackerStatus::isCaughtUp);
    }
}
