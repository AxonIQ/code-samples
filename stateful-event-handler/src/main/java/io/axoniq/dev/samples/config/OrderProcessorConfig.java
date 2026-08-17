package io.axoniq.dev.samples.config;

import org.axonframework.extension.spring.config.EventProcessorDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderProcessorConfig {

    @Bean
    public EventProcessorDefinition orderProcessorDefinition() {
        // A PooledStreamingEventProcessor is inherently multi-segment/multi-threaded (16 segments by
        // default). Restricting it to a single initial segment means there is only ever one segment
        // to claim, so only one worker thread will ever be processing for this processor - reproducing
        // AF4's TrackingEventProcessorConfiguration#forSingleThreadedProcessing() semantics.
        //
        // AF4's StreamableMessageSource#createHeadToken() maps directly onto AF5's
        // TrackingTokenSource#latestToken(...): both create a token pointing at the current end of the
        // stream, so only events published after start-up are processed and earlier events (which could
        // otherwise trigger unwanted side effects, such as re-sending commands) are skipped.
        return EventProcessorDefinition.pooledStreamingMatching("OrderProcessor")
                                       .customized(config -> config
                                               .initialSegmentCount(1)
                                               .initialToken(source -> source.latestToken(null)));
    }
}
