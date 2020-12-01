package io.axoniq.dev.samples.handler;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.handler.AccountEventHandler;
import io.axoniq.dev.samples.command.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class AccountEventHandlerTest {

    private AccountEventHandler testSubject;
    private final EmailRepository emailRepository = mock(EmailRepository.class);
    public static final java.util.UUID UUID = java.util.UUID.randomUUID();
    public static final String EMAIL_ADDRESS = "bob@gmail.com";

    @BeforeEach
    void setup() {
        testSubject = new AccountEventHandler();
    }

    @Test
    void accountCreatedTest() {
        testSubject.on(new AccountCreatedEvent(UUID, EMAIL_ADDRESS), emailRepository);
        EmailJpaEntity emailJpaEntity = new EmailJpaEntity(EMAIL_ADDRESS, UUID);
        Mockito.verify(emailRepository).save(emailJpaEntity);
    }

    @Test
    void emailAddressChangedTest() {
        when(emailRepository.findEmailJpaEntityByAccountId(UUID)).thenReturn(Optional.of(new EmailJpaEntity(
                EMAIL_ADDRESS,
                UUID)));
        testSubject.on(new EmailAddressChangedEvent(UUID, EMAIL_ADDRESS), emailRepository);
        EmailJpaEntity emailJpaEntity = new EmailJpaEntity(EMAIL_ADDRESS, UUID);
        Mockito.verify(emailRepository).delete(emailJpaEntity);
        Mockito.verify(emailRepository).save(emailJpaEntity);
    }
}
