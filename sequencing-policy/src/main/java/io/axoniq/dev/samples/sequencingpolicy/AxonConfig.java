package io.axoniq.dev.samples.sequencingpolicy;

import org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.inmemory.InMemoryTokenStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonConfig {

    /**
     * By default, the InMemoryTokenStore is not autowired since it's not 'production ready'. So, we need to explicitly
     * declare it ourselves.
     * <p>
     * Note that the {@code SequencingPolicy} beans that used to live here
     * ({@code flightIdSequencingPolicy}/{@code propertySequencingPolicy}, selected through the
     * {@code axon.eventhandling.processors.flight-time.sequencing-policy} property) have been removed: that
     * property-driven bean-lookup mechanism has no effect in Axon Framework 5. The sequencing policy is now declared
     * directly on the event handling component through the {@code @SequencingPolicy} annotation - see
     * {@link io.axoniq.dev.samples.sequencingpolicy.querymodel.FlightTimeProjector} and
     * {@link io.axoniq.dev.samples.sequencingpolicy.querymodel.PropertyFlightTimeProjector}.
     *
     * @return an InMemoryTokenStore
     */
    @Bean
    TokenStore tokenStore() {
        return new InMemoryTokenStore();
    }
}
