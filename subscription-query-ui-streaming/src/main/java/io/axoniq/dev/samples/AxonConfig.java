package io.axoniq.dev.samples;

import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.interceptors.LoggingInterceptor;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonConfig {

    @Bean
    public LoggingInterceptor<Message<?>> loggingInterceptor() {
        return new LoggingInterceptor<>();
    }

    @Autowired
    public void configureLoggingInterceptor(LoggingInterceptor<Message<?>> loggingInterceptor,
                                            EventGateway eventGateway,
                                            QueryGateway queryGateway) {
        eventGateway.registerDispatchInterceptor(loggingInterceptor);
        queryGateway.registerDispatchInterceptor(loggingInterceptor);
    }
}
