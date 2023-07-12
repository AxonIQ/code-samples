package io.axoniq.dev.samples.sequencingpolicy;

import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightEvent;
import io.axoniq.dev.samples.sequencingpolicy.coreapi.FlightId;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.async.PropertySequencingPolicy;
import org.axonframework.eventhandling.async.SequencingPolicy;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonConfig {

    /**
     * By default, the InMemoryTokenStore is not autowired since it's not 'production ready'. So, we need to explicitly
     * declare it ourselves.
     *
     * @return an InMemoryTokenStore
     */
    @Bean
    TokenStore tokenStore() {
        return new InMemoryTokenStore();
    }

    /**
     * Constructs the custom {@link FlightIdSequencingPolicy}.
     *
     * @return the custom {@link FlightIdSequencingPolicy}
     */
    @Bean
    @Qualifier("flightIdSequencingPolicy")
    @ConditionalOnProperty(value = "policy", havingValue = "custom")
    public SequencingPolicy<EventMessage<?>> flightIdSequencingPolicy() {
        return new FlightIdSequencingPolicy();
    }

    /**
     * Constructs a {@link PropertySequencingPolicy} instance.
     * <p>
     * Added to show that the {@code PropertySequencingPolicy} in essence works identical as the custom
     * {@link FlightIdSequencingPolicy}.
     *
     * @return a {@link PropertySequencingPolicy} using the {@link FlightId} to sequence on
     */
    @SuppressWarnings("rawtypes")
    @Bean
    @Qualifier("propertySequencingPolicy")
    @ConditionalOnProperty(value = "policy", havingValue = "property")
    public SequencingPolicy<EventMessage> propertySequencingPolicy() {
        return PropertySequencingPolicy.builder(FlightEvent.class, FlightId.class)
                                       .propertyName("flightId")
                                       .build();
    }
}
