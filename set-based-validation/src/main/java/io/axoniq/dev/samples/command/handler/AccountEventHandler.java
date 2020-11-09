package io.axoniq.dev.samples.command.handler;

import java.util.Optional;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.command.persistence.EmailRepository;

/**
 * Subscribing processor that updates lookup table with email addresses used in the Account.
 * Links to "Update the look-up table" in the blog
 *
 * @author Yvonne Ceelie
 */
@Component
@ProcessingGroup("emailEntity")
public class AccountEventHandler {
    @EventHandler
    public void on(AccountCreatedEvent event, EmailRepository emailRepository) {
        emailRepository.save(new EmailJpaEntity(event.getEmailAddress(), event.getAccountId()));
    }

    @EventHandler
    public void on(EmailAddressChangedEvent event, EmailRepository emailRepository) {
        Optional<EmailJpaEntity> emailEntityOptional = emailRepository.findEmailJpaEntityByAccountId(event.getAccountId());
        emailEntityOptional.ifPresent(emailRepository::delete);
        emailRepository.save(new EmailJpaEntity(event.getEmailAddress(), event.getAccountId()));
    }
}
