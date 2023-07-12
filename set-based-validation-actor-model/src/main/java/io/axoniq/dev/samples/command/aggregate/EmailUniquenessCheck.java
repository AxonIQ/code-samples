package io.axoniq.dev.samples.command.aggregate;

import io.axoniq.dev.samples.api.commands.RegisterUserWithUniqueEmailAddressCommand;
import io.axoniq.dev.samples.api.commands.RemoveUserWithUniqueEmailAddressCommand;
import io.axoniq.dev.samples.api.events.EmailAddressApprovedEvent;
import io.axoniq.dev.samples.api.events.EmailAddressRemovedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.util.Assert;

import java.util.UUID;

@Aggregate
class EmailUniquenessCheck {

    @AggregateIdentifier
    private String emailAddress;
    private UUID userId;
    private boolean claimed = false;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(RegisterUserWithUniqueEmailAddressCommand command) throws Exception {
        // A user should only be registered when the email address has not been claimed
        Assert.isTrue(!claimed, () -> "A user with this email address already exists");
        AggregateLifecycle.createNew(User.class, () -> new User(command.userId(), command.email()));
        AggregateLifecycle.apply(new EmailAddressApprovedEvent(command.email(), command.userId()));
    }

    @CommandHandler
    public void handle(RemoveUserWithUniqueEmailAddressCommand command) {
        Assert.isTrue(claimed, () -> "This email address has not been claimed");
        AggregateLifecycle.apply(new EmailAddressRemovedEvent(command.email(), userId));
    }

    @EventSourcingHandler
    public void on(EmailAddressApprovedEvent event) {
        this.emailAddress = event.emailAddress();
        this.claimed = true;
        this.userId = event.userId();
    }

    @EventSourcingHandler
    public void on(EmailAddressRemovedEvent event) {
        this.claimed = false;
    }

    public EmailUniquenessCheck() {
        // needed by Axon
    }
}
