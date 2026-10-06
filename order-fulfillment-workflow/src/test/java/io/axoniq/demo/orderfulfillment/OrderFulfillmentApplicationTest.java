package io.axoniq.demo.orderfulfillment;

import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.messaging.core.MessageDispatchInterceptor;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
               properties = {"axon.axonserver.enabled=false",
                       "spring.datasource.url=jdbc:h2:mem:order-fulfillment-test;DB_CLOSE_DELAY=-1"})
@AutoConfigureTestRestTemplate
@Import(OrderFulfillmentApplicationTest.InMemoryStorage.class)
class OrderFulfillmentApplicationTest extends AbstractOrderFulfillmentTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class InMemoryStorage {

        @Bean
        EventStorageEngine eventStorageEngine() {
            return new InMemoryEventStorageEngine();
        }

        @Bean
        MessageDispatchInterceptor<EventMessage> inMemoryEventConversion(EventConverter converter) {
            // Axon Server attaches a converter when reading persisted events. The in-memory
            // store retains the original message, so attach it before publication here.
            return (message, context, chain) -> chain.proceed(
                    new GenericEventMessage(message.identifier(), message.type(), message.payload(),
                                            message.metadata(), message.timestamp()).withConverter(converter), context);
        }
    }
}
