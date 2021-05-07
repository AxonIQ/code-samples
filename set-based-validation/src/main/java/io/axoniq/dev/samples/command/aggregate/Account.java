package io.axoniq.dev.samples.command.aggregate;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.AlterEmailAddressCommand;
import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork;
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
    public Account(CreateAccountCommand command, EmailRepository emailRepository) {
        apply(new AccountCreatedEvent(command.getAccountId(), command.getEmailAddress()));
        // This hooks into the unit of work to save the name of the account after the event is saved
        CurrentUnitOfWork.get().afterCommit(uow -> emailRepository
                .save(new EmailJpaEntity(command.getEmailAddress(), command.getAccountId())));
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
        CurrentUnitOfWork.get().afterCommit(unitOfWork -> updateEmailAddress(emailRepository, command));
    }

    private void updateEmailAddress(EmailRepository emailRepository, AlterEmailAddressCommand command) {
        emailRepository.findEmailJpaEntityByAccountId(command.getAccountId()).ifPresent(emailRepository::delete);
        emailRepository.save(new EmailJpaEntity(command.getUpdatedEmailAddress(), command.getAccountId()));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        this.accountId = event.getAccountId();
        this.emailAddress = event.getEmailAddress();
    }

    @EventSourcingHandler
    public void on(EmailAddressChangedEvent event) {
        this.emailAddress = event.getEmailAddress();
    }

    public Account() {
        // Required by Axon
    }
}
