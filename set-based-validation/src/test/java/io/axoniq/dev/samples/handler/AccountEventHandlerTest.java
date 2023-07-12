package io.axoniq.dev.samples.handler;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.handler.AccountEventHandler;
import io.axoniq.dev.samples.command.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.mockito.Mockito.*;

class AccountEventHandlerTest {

    private static final java.util.UUID UUID = java.util.UUID.randomUUID();
    private static final String EMAIL_ADDRESS = "bob@gmail.com";

    private final EmailRepository emailRepository = mock(EmailRepository.class);

    private AccountEventHandler testSubject;

    @BeforeEach
    void setup() {
        testSubject = new AccountEventHandler();
    }

    @Test
    void accountCreatedTest() {
        testSubject.on(new AccountCreatedEvent(UUID, EMAIL_ADDRESS), emailRepository);
        EmailJpaEntity emailJpaEntity = new EmailJpaEntity(EMAIL_ADDRESS, UUID);
        verify(emailRepository).save(emailJpaEntity);
    }

    @Test
    void emailAddressChangedTest() {
        when(emailRepository.findEmailJpaEntityByAccountId(UUID)).thenReturn(Optional.of(new EmailJpaEntity(
                EMAIL_ADDRESS,
                UUID)));
        testSubject.on(new EmailAddressChangedEvent(UUID, EMAIL_ADDRESS), emailRepository);
        EmailJpaEntity emailJpaEntity = new EmailJpaEntity(EMAIL_ADDRESS, UUID);
        verify(emailRepository).delete(emailJpaEntity);
        verify(emailRepository).save(emailJpaEntity);
    }
}
