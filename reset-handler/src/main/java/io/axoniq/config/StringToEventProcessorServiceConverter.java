package io.axoniq.config;

import io.axoniq.service.FrameworkEventProcessorService;
import io.axoniq.service.ServerConnectorEventProcessorService;
import io.axoniq.service.EventProcessorService;
import io.axoniq.service.RestEventProcessorService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/*
    Extract the service using the specified method to reset the token.
    This allows for cleaner signatures in the controllers and the services
 */
@Component
public class StringToEventProcessorServiceConverter implements Converter<String, EventProcessorService> {

    final RestEventProcessorService restEventProcessorService;
    final FrameworkEventProcessorService frameworkEventProcessorService;
    final ServerConnectorEventProcessorService serverConnectorEventProcessorService;

    public StringToEventProcessorServiceConverter(RestEventProcessorService restEventProcessorService, FrameworkEventProcessorService frameworkEventProcessorService, ServerConnectorEventProcessorService serverConnectorEventProcessorService) {
        this.restEventProcessorService = restEventProcessorService;
        this.frameworkEventProcessorService = frameworkEventProcessorService;
        this.serverConnectorEventProcessorService = serverConnectorEventProcessorService;
    }

    /*
        Match the passed string against a set of known constants.
        This is not elegant but does get its job done for the sample.
     */
    @Override
    public EventProcessorService convert(String from) {
        switch (from){
            case "server": return serverConnectorEventProcessorService;
            case "rest": return restEventProcessorService;
            case "grpc": throw new IllegalArgumentException();
            case "framework": return frameworkEventProcessorService;
            default: throw new IllegalArgumentException();
        }
    }
}