package io.axoniq.config;

import io.axoniq.service.FrameworkEventProcessorService;
import io.axoniq.service.ServerConnectorEventProcessorService;
import io.axoniq.service.EventProcessorService;
import io.axoniq.service.RestEventProcessorService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToEventProcessorServiceConverter implements Converter<String, EventProcessorService> {

    final RestEventProcessorService restEventProcessorService;
    final FrameworkEventProcessorService frameworkEventProcessorRestController;
    final ServerConnectorEventProcessorService serverConnectorRestController;

    public StringToEventProcessorServiceConverter(RestEventProcessorService restEventProcessorService, FrameworkEventProcessorService frameworkEventProcessorRestController, ServerConnectorEventProcessorService serverConnectorRestController) {
        this.restEventProcessorService = restEventProcessorService;
        this.frameworkEventProcessorRestController = frameworkEventProcessorRestController;
        this.serverConnectorRestController = serverConnectorRestController;
    }

    @Override
    public EventProcessorService convert(String from) {
        switch (from){
            case "server": return serverConnectorRestController;
            case "rest": return restEventProcessorService;
            case "grpc": throw new IllegalArgumentException();
            case "framework": return frameworkEventProcessorRestController;
            default: throw new IllegalArgumentException();
        }
    }
}