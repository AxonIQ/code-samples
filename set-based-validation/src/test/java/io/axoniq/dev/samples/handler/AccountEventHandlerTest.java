package io.axoniq.dev.samples.handler;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.handler.AccountEventHandler;
import io.axoniq.dev.samples.command.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.*;

public class AccountEventHandlerTest {

    private AccountEventHandler testSubject;
    private final EmailRepository emailRepository = mock(EmailRepository.class);
    public static final UUID ACCOUNT_ID = randomUUID();
    public static final String EMAIL_ADDRESS = "bob@gmail.com";
    private static final String EMAIL_ADDRESS_CHANGED = "bob2@gmail.com";

    @BeforeEach
    void setup() {
        testSubject = new AccountEventHandler();
    }

    @Test
    void accountCreatedTest() {
        when(emailRepository.findEmailJpaEntityByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        testSubject.on(new AccountCreatedEvent(ACCOUNT_ID, EMAIL_ADDRESS), emailRepository);
        EmailJpaEntity emailJpaEntity = new EmailJpaEntity(EMAIL_ADDRESS, ACCOUNT_ID);
        verify(emailRepository).findEmailJpaEntityByAccountId(ACCOUNT_ID);
        verify(emailRepository).save(emailJpaEntity);
    }

    @Test
    void emailAddressChangedTest() {
        when(emailRepository.findEmailJpaEntityByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(new EmailJpaEntity(
                EMAIL_ADDRESS,
                ACCOUNT_ID)));
        testSubject.on(new EmailAddressChangedEvent(ACCOUNT_ID, EMAIL_ADDRESS_CHANGED), emailRepository);
        EmailJpaEntity emailJpaEntity = new EmailJpaEntity(EMAIL_ADDRESS_CHANGED, ACCOUNT_ID);
        verify(emailRepository).findEmailJpaEntityByAccountId(ACCOUNT_ID);
        verify(emailRepository).save(emailJpaEntity);
    }
}
