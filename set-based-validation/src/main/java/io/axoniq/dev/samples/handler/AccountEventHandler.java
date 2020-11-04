package io.axoniq.dev.samples.handler;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.persistence.EmailRepository;

@Component
@ProcessingGroup("emailEntity")
public class AccountEventHandler {
    @EventHandler
    public void on (AccountCreatedEvent event, EmailRepository emailRepository){
        emailRepository.save(new EmailJpaEntity(event.getEmailAddress(), event.getAccountId()));
    }

    @EventHandler
    public void on (EmailAddressChangedEvent event, EmailRepository emailRepository){
        EmailJpaEntity emailJpaEntity = emailRepository.findEmailJpaEntityByAccountId(event.getAccountId());
        if (emailJpaEntity != null) {
            // Delete the former registered email address
            emailRepository.delete(emailRepository.findEmailJpaEntityByAccountId(event.getAccountId()));
        }
        emailRepository.save(new EmailJpaEntity(event.getEmailAddress(), event.getAccountId()));
    }

}
