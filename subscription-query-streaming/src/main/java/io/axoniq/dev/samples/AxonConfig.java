package io.axoniq.dev.samples;

import org.axonframework.config.ConfigurerModule;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.lifecycle.Phase;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.interceptors.LoggingInterceptor;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Basic Axon configuration adding the {@link LoggingInterceptor} to the {@link QueryGateway} and {@link EventGateway}.
 * This provides easy logging of all the messages being published, ensuring that portion of the application works.
 *
 * @author Steven van Beelen
 */
@Configuration
public class AxonConfig {

    @Bean
    public LoggingInterceptor<Message<?>> loggingInterceptor() {
        return new LoggingInterceptor<>();
    }

    @SuppressWarnings("resource")
    @Bean
    public ConfigurerModule loggingInterceptorConfigurerModule(LoggingInterceptor<Message<?>> loggingInterceptor) {
        return configurer -> configurer.onInitialize(config -> config.onStart(Phase.OUTBOUND_QUERY_CONNECTORS, () -> {
            config.eventGateway().registerDispatchInterceptor(loggingInterceptor);
            config.queryGateway().registerDispatchInterceptor(loggingInterceptor);
        }));
    }
}
