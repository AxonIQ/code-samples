package io.axoniq.dev.samples.command.aggregate;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.AlterEmailAddressCommand;
import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
public class Account {

    @AggregateIdentifier
    private UUID accountId;
    private String emailAddress;

    @CommandHandler
    public Account(CreateAccountCommand command) {
        apply(new AccountCreatedEvent(command.accountId(), command.emailAddress()));
    }

    /**
     * Uses the parameter resolver for email address exists. Links to "Validation using a parameter resolver" in the
     * blog
     */
    @CommandHandler
    public void handle(ChangeEmailAddressCommand command, Boolean emailAddressExists) {
        if (emailAddressExists) {
            throw new IllegalStateException(String.format("Account with email address %s already exists",
                                                          command.updatedEmailAddress()));
        }
        if (emailAddress.equals(command.updatedEmailAddress())) {
            throw new IllegalStateException(String.format("Email address %s is already used for account with id %s ",
                                                          command.updatedEmailAddress(), accountId));
        }
        apply(new EmailAddressChangedEvent(command.accountId(), command.updatedEmailAddress()));
    }

    @CommandHandler
    public void handle(AlterEmailAddressCommand command, EmailRepository emailRepository) {
        if (emailRepository.existsById(command.updatedEmailAddress())) {
            throw new IllegalStateException(String.format("Account with email address %s already exists",
                                                          command.updatedEmailAddress()));
        }
        if (emailAddress.equals(command.updatedEmailAddress())) {
            throw new IllegalStateException(String.format("Email address %s is already used for account with id %s ",
                                                          command.updatedEmailAddress(), accountId));
        }

        apply(new EmailAddressChangedEvent(command.accountId(), command.updatedEmailAddress()));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        this.accountId = event.accountId();
        this.emailAddress = event.emailAddress();
    }

    @EventSourcingHandler
    public void on(EmailAddressChangedEvent event) {
        this.emailAddress = event.emailAddress();
    }

    public Account() {
        // Required by Axon
    }
}
