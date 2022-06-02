package io.axoniq.service;
        
import io.axoniq.axonserver.connector.AxonServerConnection;
import io.axoniq.axonserver.connector.AxonServerConnectionFactory;
import io.axoniq.axonserver.connector.admin.AdminChannel;
import io.axoniq.axonserver.connector.control.ControlChannel;
import io.axoniq.axonserver.connector.impl.ContextConnection;
import io.axoniq.axonserver.grpc.admin.EventProcessor;
//import org.axonframework.axonserver.connector.AxonServerConnectionManager;
//import org.axonframework.config.Configuration;
//import org.axonframework.eventhandling.StreamingEventProcessor;
import io.axoniq.axonserver.grpc.admin.Result;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
  * @author Sara Pellegrini
  * @since 1.1
  */
@Service
public class ServerConnectorEventProcessorService implements EventProcessorService {
    private final Supplier<String> contextSupplier;
    private final Supplier<String> componentSupplier;
    private final Configuration configuration;

    public ServerConnectorEventProcessorService(@Value("${axon.axonserver.context}") String context,
                                     @Value("${axon.axonserver.component-name}") String component,Configuration configuration) {
        this.configuration = configuration;
        this.contextSupplier = () -> context;
        this.componentSupplier = () -> component;
    }

    @Override
    public Mono<Void> pause(String processorName) {
        System.out.println("Pause via server");
        return null;
    }

    @Override
    public Mono<Void> start(String processorName) {
        System.out.println("start via server");
        return null;
    }

    @Override
    public Mono<Void> reset(String processorName) {

        AxonServerConnectionFactory connectionFactory = AxonServerConnectionFactory.forClient(componentSupplier.get()).build();
        ContextConnection contextConnection = (ContextConnection) connectionFactory.connect(contextSupplier.get());

        AdminChannel adminChannel = contextConnection.adminChannel();

        StreamingEventProcessor eventProcessor = configuration.eventProcessingConfiguration()
                .eventProcessorByProcessingGroup(
                        processorName,
                        StreamingEventProcessor.class)
                .orElseThrow(IllegalArgumentException::new);

        String tokenStoreIdentifier = eventProcessor.getTokenStoreIdentifier();

        adminChannel
                .pauseEventProcessor(processorName, tokenStoreIdentifier)
                .thenApply(res -> {
                    if( res == Result.ACCEPTED){
                        throw new RuntimeException("We currently do not support the old asynchronous stop mechanism");
                    }
                    return true; //TODO do
                })
                .thenRun(eventProcessor::resetTokens)
                .thenRun(() -> adminChannel.startEventProcessor(processorName, tokenStoreIdentifier));
        return null;
    }


}