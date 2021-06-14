package io.axoniq.dev.samples.saga;

import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.messaging.StreamableMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessOrderSagaConfig {

    @Autowired
    public void configure(EventProcessingConfigurer configurer) {
        configurer.registerPooledStreamingEventProcessor
                ("ProcessOrderSagaProcessor",
                 org.axonframework.config.Configuration::eventStore,
                 (configuration, builder) -> builder.initialToken(
                         StreamableMessageSource::createTailToken));
    }

}
