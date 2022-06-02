package io.axoniq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Add a Converter from String to EventProcessorService.
 * We can for example map `framework` to a FrameworkEventProcessorService
 */
@Configuration
public class WebConfig implements WebFluxConfigurer {

    final StringToEventProcessorServiceConverter converter;

    public WebConfig(StringToEventProcessorServiceConverter converter) {
        this.converter = converter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(converter);
    }
}