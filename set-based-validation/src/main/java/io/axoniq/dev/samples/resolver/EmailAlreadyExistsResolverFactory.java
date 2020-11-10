package io.axoniq.dev.samples.resolver;

import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.annotation.ParameterResolver;
import org.axonframework.messaging.annotation.ParameterResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

/**
 * This parameter resolver resolves to true if an account aggregate with email address already exists. Links to the
 * "Validation using a Parameter Resolver" section in the blog.
 *
 * @author Yvonne Ceelie
 */
@Component
public class EmailAlreadyExistsResolverFactory implements ParameterResolver<Boolean>, ParameterResolverFactory {

    private final EmailRepository emailRepository;

    @Autowired
    public EmailAlreadyExistsResolverFactory(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @Override
    public ParameterResolver createInstance(Executable executable, Parameter[] parameters, int parameterIndex) {
        if (Boolean.class.equals(parameters[parameterIndex].getType())) {
            return this;
        }
        return null;
    }

    @Override
    public Boolean resolveParameterValue(Message<?> message) {
        if (matches(message)) {
            String emailAddress = ((ChangeEmailAddressCommand) message.getPayload()).getUpdatedEmailAddress();
            return emailRepository.findById(emailAddress).isPresent();
        }
        throw new IllegalArgumentException("Message payload not of type ChangeEmailAddressCommand");
    }

    @Override
    public boolean matches(Message<?> message) {
        return ChangeEmailAddressCommand.class.isAssignableFrom(message.getPayloadType());
    }

    @Override
    public Class<?> supportedPayloadType() {
        return ChangeEmailAddressCommand.class;
    }
}
