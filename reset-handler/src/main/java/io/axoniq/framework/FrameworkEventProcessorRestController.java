package io.axoniq.framework;

import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("framework")
public class FrameworkEventProcessorRestController {

    private final Configuration configuration;

    public FrameworkEventProcessorRestController(Configuration configuration) {
        this.configuration = configuration;
    }

    @GetMapping("reset/{processorName}")
    public ResponseEntity<Void> reset(@PathVariable String processorName) {
        Assert.hasLength(processorName, "Processing Group is mandatory and can't be empty!");

        configuration.eventProcessingConfiguration()
                     .eventProcessorByProcessingGroup(processorName,
                                                      StreamingEventProcessor.class)
                     .ifPresent(streamingEventProcessor -> {
                         if (streamingEventProcessor.supportsReset()) {
                             streamingEventProcessor.shutDown();
                             streamingEventProcessor.resetTokens();
                             streamingEventProcessor.start();
                         }
                     });

        return ResponseEntity.ok().build();
    }
}
