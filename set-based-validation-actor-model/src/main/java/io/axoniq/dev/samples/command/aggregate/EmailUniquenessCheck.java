package io.axoniq.dev.samples.command.aggregate;

import io.axoniq.dev.samples.api.commands.RegisterUserWithUniqueEmailAddress;
import io.axoniq.dev.samples.api.commands.RemoveUserWithUniqueEmailAddress;
import io.axoniq.dev.samples.api.events.EmailAddressApproved;
import io.axoniq.dev.samples.api.events.EmailAddressRemoved;
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
public class EmailUniquenessCheck {

    @AggregateIdentifier
    private String emailAddress;

    private UUID userId;

    private boolean claimed = false;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(RegisterUserWithUniqueEmailAddress command) throws Exception {
        // A user should only be registered when the email address has not been claimed
        Assert.isTrue(!claimed, () -> "A user with this email address already exists");
        AggregateLifecycle.createNew(User.class, () -> new User(command.getUserId(), command.getEmail()));
        AggregateLifecycle.apply(new EmailAddressApproved(command.getEmail(), command.getUserId()));
    }

    @CommandHandler
    public void handle(RemoveUserWithUniqueEmailAddress command) {
        Assert.isTrue(claimed, () -> "This email address has not been claimed");
        AggregateLifecycle.apply(new EmailAddressRemoved(command.getEmail(), userId));
    }

    @EventSourcingHandler
    public void on(EmailAddressApproved event) {
        this.emailAddress = event.getEmailAddress();
        this.claimed = true;
        this.userId = event.getUserId();
    }

    @EventSourcingHandler
    public void on(EmailAddressRemoved event) {
        this.claimed = false;
    }

    public EmailUniquenessCheck() {
        // needed by Axon
    }
}
