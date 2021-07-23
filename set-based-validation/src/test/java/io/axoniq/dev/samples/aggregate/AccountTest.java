package io.axoniq.dev.samples.aggregate;

import io.axoniq.dev.samples.api.AccountCreatedEvent;
import io.axoniq.dev.samples.api.AlterEmailAddressCommand;
import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.api.EmailAddressChangedEvent;
import io.axoniq.dev.samples.command.aggregate.Account;
import io.axoniq.dev.samples.command.persistence.EmailJpaEntity;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import io.axoniq.dev.samples.resolver.EmailAlreadyExistsResolverFactory;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class AccountTest {

    private static final UUID UUID = java.util.UUID.randomUUID();
    private static final String EMAIL_ADDRESS = "bob@gmail.com";
    private static final String EMAIL_ADDRESS_CHANGED = "bob2@gmail.com";

    private FixtureConfiguration<Account> fixture;
    private final EmailRepository emailRepository = mock(EmailRepository.class);
    private final EmailAlreadyExistsResolverFactory emailAlreadyExistsResolverFactory =
            new EmailAlreadyExistsResolverFactory(emailRepository);

    @BeforeEach
    void setup() {
        fixture = new AggregateTestFixture<>(Account.class);
        fixture.registerInjectableResource(new EmailJpaEntity(EMAIL_ADDRESS, UUID));
        fixture.registerInjectableResource(emailRepository);
        fixture.registerParameterResolverFactory(emailAlreadyExistsResolverFactory);
    }

    @Test
    void shouldCreateAccount() {
        fixture.givenNoPriorActivity()
               .when(new CreateAccountCommand(UUID, EMAIL_ADDRESS))
               .expectEvents(new AccountCreatedEvent(UUID, EMAIL_ADDRESS));
        //Check if the email address is saved in the after commit of the Unit Of Work
        verify(emailRepository).save(new EmailJpaEntity(EMAIL_ADDRESS, UUID));
    }

    @Test
    void shouldUpdateEmailAddress() {
        when(emailRepository.findById(EMAIL_ADDRESS_CHANGED)).thenReturn(Optional.empty());

        fixture.given(new AccountCreatedEvent(UUID, EMAIL_ADDRESS))
               .when(new ChangeEmailAddressCommand(UUID, EMAIL_ADDRESS_CHANGED))
               .expectEvents(new EmailAddressChangedEvent(UUID, EMAIL_ADDRESS_CHANGED));
    }

    @Test
    void shouldNotUpdateDuplicateEmailAddress() {
        when(emailRepository.findById(EMAIL_ADDRESS)).thenReturn(Optional.of(new EmailJpaEntity(EMAIL_ADDRESS, UUID)));
        fixture.given(new AccountCreatedEvent(UUID, EMAIL_ADDRESS))
               .when(new ChangeEmailAddressCommand(UUID, EMAIL_ADDRESS))
               .expectNoEvents();
    }

    @Test
    void shouldAlterEmailAddress() {
        when(emailRepository.findEmailJpaEntityByAccountId(UUID)).thenReturn(Optional.of(new EmailJpaEntity(
                EMAIL_ADDRESS,
                UUID)));

        fixture.given(new AccountCreatedEvent(UUID, EMAIL_ADDRESS))
               .when(new AlterEmailAddressCommand(UUID, EMAIL_ADDRESS_CHANGED))
               .expectEvents(new EmailAddressChangedEvent(UUID, EMAIL_ADDRESS_CHANGED));

        //Check if the email address is updated in the after commit of the Unit Of Work
        verify(emailRepository).findEmailJpaEntityByAccountId(UUID);
        verify(emailRepository).delete(new EmailJpaEntity(EMAIL_ADDRESS, UUID));
        verify(emailRepository).save(new EmailJpaEntity(EMAIL_ADDRESS_CHANGED, UUID));
    }

    @Test
    void shouldNotAlterDuplicateEmailAddress() {
        when(emailRepository.existsById(EMAIL_ADDRESS)).thenReturn(true);
        fixture.given(new AccountCreatedEvent(UUID, EMAIL_ADDRESS))
               .when(new AlterEmailAddressCommand(UUID, EMAIL_ADDRESS))
               .expectNoEvents();
    }
}