package io.axoniq.dev.samples.validator;

import io.axoniq.dev.samples.command.persistence.EmailRepository;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.EventTrackerStatus;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmailUniquenessValidator {

    EmailRepository emailRepository;
    Configuration configuration;

    public EmailUniquenessValidator(EmailRepository emailRepository, Configuration configuration) {
        this.emailRepository = emailRepository;
        this.configuration = configuration;
    }

    public boolean emailAddressExists(String emailAdress){
        trackingEventProcessorChecks();
        return emailRepository.existsById(emailAdress);
    }

    public void validateEmailAddress(String emailAdress) {
        trackingEventProcessorChecks();
        if (emailRepository.existsById(emailAdress)) {
            throw new IllegalStateException(String.format("Account with email address %s already exists", emailAdress));
        }
        return;
    }


    private void trackingEventProcessorChecks() {
        Optional<TrackingEventProcessor> emailEventProcessorOptional = configuration
                .eventProcessingConfiguration().eventProcessorByProcessingGroup("emailEntityProcessor",
                                                                                TrackingEventProcessor.class);
        if (emailEventProcessorOptional.isPresent()) {
            checkEventProcessor(emailEventProcessorOptional.get());
        } else {
            throw new IllegalStateException("EmailEntityProcessor is not configured");
        }
    }

    private void checkEventProcessor(TrackingEventProcessor trackingEventProcessor) {
        if (trackingEventProcessor.isReplaying()) {
            throw new IllegalStateException("EmailEntityProcessor is replaying");
        }
        if (!trackingEventProcessor.processingStatus().values().stream().allMatch(EventTrackerStatus::isCaughtUp)) {
            throw new IllegalStateException("EmailEntityProcessor is not caught up");
        }
    }
}
