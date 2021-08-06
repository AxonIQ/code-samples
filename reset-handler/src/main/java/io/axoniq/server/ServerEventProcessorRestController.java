package io.axoniq.server;

import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("server")
public class ServerEventProcessorRestController {

    private final EventProcessorService eventProcessorService;
    private final Configuration configuration;


    public ServerEventProcessorRestController(EventProcessorService eventProcessorService,
                                              Configuration configuration) {
        this.eventProcessorService = eventProcessorService;
        this.configuration = configuration;
    }

    @GetMapping("start/{processorName}")
    public Mono<Void> start(@PathVariable String processorName) {
        return eventProcessorService.start(processorName);
    }

    @GetMapping("pause/{processorName}")
    public Mono<Void> pause(@PathVariable String processorName) {
        return eventProcessorService.pause(processorName);
    }

    @GetMapping("reset/{processorName}")
    public Mono<Void> reset(@PathVariable String processorName) {
        Assert.hasLength(processorName, "Processor Name is mandatory and can't be empty!");
        StreamingEventProcessor eventProcessor = configuration.eventProcessingConfiguration()
                                                              .eventProcessorByProcessingGroup(
                                                                      processorName,
                                                                      StreamingEventProcessor.class)
                                                              .orElseThrow(IllegalArgumentException::new);

        return eventProcessorService.pause(processorName)
                                    .then(eventProcessorService.awaitTermination(processorName))
                                    .then(Mono.<Void>fromRunnable(eventProcessor::resetTokens))
                                    .then(eventProcessorService.start(processorName));
    }
}
