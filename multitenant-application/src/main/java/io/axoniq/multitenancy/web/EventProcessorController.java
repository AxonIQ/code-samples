/*
 * Copyright (c) 2020-2020. AxonIQ
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;)
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.axoniq.multitenancy.web;

import io.axoniq.axonserver.connector.AxonServerConnection;
import io.axoniq.axonserver.grpc.admin.CreateContextRequest;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.axonframework.extensions.multitenancy.components.eventhandeling.MultiTenantEventProcessor;
import org.axonframework.messaging.StreamableMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;


@RestController
public class EventProcessorController {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Configuration configuration;
    private final AxonServerConnectionManager axonServerConnectionManager;


    public EventProcessorController(Configuration configuration,
                                    AxonServerConnectionManager axonServerConnectionManager) {
        this.configuration = configuration;
        this.axonServerConnectionManager = axonServerConnectionManager;
    }

    @PostMapping(path = "/reset/{processingGroup}")
    public void reset(@PathVariable String processingGroup) {
        Assert.hasLength(processingGroup, "Processing Group is mandatory and can't be empty!");

        configuration.eventProcessingConfiguration()
                     .eventProcessorByProcessingGroup(processingGroup,
                                                      StreamingEventProcessor.class)
                     .ifPresent(streamingEventProcessor -> {
                         if (streamingEventProcessor.supportsReset()) {
                             logger.info("Trigger ResetTriggeredEvent for processingGroup {}", processingGroup);
                             streamingEventProcessor.shutDown();
                             streamingEventProcessor.resetTokens(StreamableMessageSource::createTailToken);
                             streamingEventProcessor.start();
                         }
                     });
    }

    @PostMapping(path = "/reset")
    public void resetAll() {
        configuration.eventProcessingConfiguration()
                .eventProcessors().values().stream().filter(instance -> instance instanceof MultiTenantEventProcessor)
                .findFirst()
                .ifPresent(mtEventProcessor -> {
                    ((MultiTenantEventProcessor) mtEventProcessor).tenantSegments().values().stream()
                            .filter(i -> i instanceof StreamingEventProcessor)
                            .map(i-> (StreamingEventProcessor) i)
                            .forEach(eventProcessor -> {
                                if (eventProcessor.supportsReset()) {
                                    eventProcessor.shutDown();
                                    eventProcessor.resetTokens(StreamableMessageSource::createTailToken);
                                    eventProcessor.start();
                                }
                            });
                });
    }
}
