package io.axoniq.dev.samples.command.handler;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Tracking event processor that updates lookup table with email addresses used in the Account. Links to the "Update the
 * look-up table" section, in this [set based validation blog](https://axoniq.io/blog-overview/set-based-validation)
 *
 * @author Yvonne Ceelie
 */
@Component
@ProcessingGroup("emailEntity")
public class AccountEventHandler {

    @EventHandler
    public void on(AccountCreatedEvent event, EmailRepository emailRepository) {
        if (!emailRepository.findEmailJpaEntityByAccountId(event.getAccountId()).isPresent()) {
            emailRepository.save(new EmailJpaEntity(event.getEmailAddress(), event.getAccountId()));
        }
    }

    @EventHandler
    public void on(EmailAddressChangedEvent event, EmailRepository emailRepository) {
        emailRepository.findEmailJpaEntityByAccountId(event.getAccountId())
                       .ifPresent(emailJpaEntity -> updateEmailAddress(emailJpaEntity, event, emailRepository));
    }

    private void updateEmailAddress(EmailJpaEntity emailJpaEntity, EmailAddressChangedEvent event,
                                    EmailRepository emailRepository) {
        if (!event.getEmailAddress().equals(emailJpaEntity.emailAddress)) {
            emailRepository.save(new EmailJpaEntity(event.getEmailAddress(), event.getAccountId()));
        }
    }
}
