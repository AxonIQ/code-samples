package io.axoniq.controller;

import io.axoniq.service.EventProcessorService;
import org.axonframework.config.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Uses the EventProcessorService provided to it based oin the supplied method
 */
@RestController
public class EventProcessorRestController {

    @GetMapping("{method}/start/{processorName}")
    public Mono<Void> start(@PathVariable("method") EventProcessorService service, @PathVariable String processorName) {
        Assert.hasText(processorName, "Processing Group is mandatory and can't be empty!");
        return service.start(processorName);
    }

    @GetMapping("{method}/pause/{processorName}")
    public Mono<Void> pause(@PathVariable("method") EventProcessorService service, @PathVariable String processorName) {
        Assert.hasText(processorName, "Processing Group is mandatory and can't be empty!");
        return service.pause(processorName);
    }

    @GetMapping("{method}/reset/{processorName}")
    public Mono<Void> reset(@PathVariable("method") EventProcessorService service, @PathVariable String processorName) {
        Assert.hasText(processorName, "Processing Group is mandatory and can't be empty!");
        return service.reset(processorName);
    }

}
