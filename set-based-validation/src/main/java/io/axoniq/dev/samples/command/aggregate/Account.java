package io.axoniq.dev.samples.command.aggregate;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.AlterEmailAddressCommand;
import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CommandHandlerInterceptor;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
public class Account {

    @AggregateIdentifier
    private UUID accountId;
    private String emailAddress;
    private boolean closed;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @CommandHandler
    public Account(CreateAccountCommand command) {
        apply(new AccountCreatedEvent(command.getAccountId(), command.getEmailAddress()));
    }

    @CommandHandlerInterceptor
    public Object interceptRunDeriveCommand(ChangeEmailAddressCommand command, InterceptorChain interceptorChain)
            throws Exception {
        logger.info("CommandHandlerInterceptor intercepting {}", command);
        if (this.closed) {
            logger.info("Ignoring command");
            throw new IllegalStateException("Application is closed");
        }
        return interceptorChain.proceed();
    }

    /**
     * Uses the parameter resolver for email address exists. Links to "Validation using a parameter resolver" in the
     * blog
     */
    @CommandHandler
    public void handle(ChangeEmailAddressCommand command, Boolean emailAddressExists) {
        if (emailAddressExists) {
            throw new IllegalStateException(String.format("Account with email address %s already exists",
                                                          command.getUpdatedEmailAddress()));
        }
        if (emailAddress.equals(command.getUpdatedEmailAddress())) {
            throw new IllegalStateException(String.format("Email address %s is already used for account with id %s ",
                                                          command.getUpdatedEmailAddress(), accountId));
        }
        apply(new EmailAddressChangedEvent(command.getAccountId(), command.getUpdatedEmailAddress()));
    }

    @CommandHandler
    public void handle(AlterEmailAddressCommand command, EmailRepository emailRepository) {
        if (emailRepository.existsById(command.getUpdatedEmailAddress())) {
            throw new IllegalStateException(String.format("Account with email address %s already exists",
                                                          command.getUpdatedEmailAddress()));
        }
        if (emailAddress.equals(command.getUpdatedEmailAddress())) {
            throw new IllegalStateException(String.format("Email address %s is already used for account with id %s ",
                                                          command.getUpdatedEmailAddress(), accountId));
        }

        apply(new EmailAddressChangedEvent(command.getAccountId(), command.getUpdatedEmailAddress()));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        this.accountId = event.getAccountId();
        this.emailAddress = event.getEmailAddress();
        this.closed = true;
    }

    @EventSourcingHandler
    public void on(EmailAddressChangedEvent event) {
        this.emailAddress = event.getEmailAddress();
    }

    public Account() {
        // Required by Axon
    }
}
