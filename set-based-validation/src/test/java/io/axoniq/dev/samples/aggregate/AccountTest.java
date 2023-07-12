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

class AccountTest {

    private static final UUID UUID = java.util.UUID.randomUUID();
    private static final String EMAIL_ADDRESS = "bob@gmail.com";
    private static final String EMAIL_ADDRESS_CHANGED = "bob2@gmail.com";

    private EmailRepository emailRepository;

    private FixtureConfiguration<Account> fixture;

    @BeforeEach
    void setup() {
        emailRepository = mock(EmailRepository.class);
        EmailAlreadyExistsResolverFactory emailAlreadyExistsResolverFactory =
                new EmailAlreadyExistsResolverFactory(emailRepository);

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
        fixture.given(new AccountCreatedEvent(UUID, EMAIL_ADDRESS))
               .when(new AlterEmailAddressCommand(UUID, EMAIL_ADDRESS_CHANGED))
               .expectEvents(new EmailAddressChangedEvent(UUID, EMAIL_ADDRESS_CHANGED));
    }

    @Test
    void shouldNotAlterDuplicateEmailAddress() {
        when(emailRepository.existsById(EMAIL_ADDRESS)).thenReturn(true);
        fixture.given(new AccountCreatedEvent(UUID, EMAIL_ADDRESS))
               .when(new AlterEmailAddressCommand(UUID, EMAIL_ADDRESS))
               .expectNoEvents();
    }
}