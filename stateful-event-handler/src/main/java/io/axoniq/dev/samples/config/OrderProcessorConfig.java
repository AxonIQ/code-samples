package io.axoniq.dev.samples.config;

import org.axonframework.config.ConfigurerModule;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.messaging.StreamableMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderProcessorConfig {

    @Bean
    public ConfigurerModule configureOrderProcessor() {
        TrackingEventProcessorConfiguration tepConfig =
                TrackingEventProcessorConfiguration.forSingleThreadedProcessing()
                        .andInitialTrackingToken(StreamableMessageSource::createTailToken);
        return configurer -> configurer.eventProcessing().registerTrackingEventProcessorConfiguration("OrderProcessor", c -> tepConfig);
    }
}
