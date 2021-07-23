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
 * look-up table" section in the blog.
 *
 * @author Yvonne Ceelie
 */
@Component
@ProcessingGroup("emailEntityProcessor")
public class AccountEventHandler {

    @EventHandler
    public void on(AccountCreatedEvent event, EmailRepository emailRepository) throws InterruptedException {
        emailRepository.save(new EmailJpaEntity(event.getEmailAddress(), event.getAccountId()));
        Thread.sleep(60000);
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
