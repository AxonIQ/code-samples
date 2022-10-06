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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;


@Service
public class CreateTenantService {

    private final AxonServerConnectionManager axonServerConnectionManager;


    public CreateTenantService(AxonServerConnectionManager axonServerConnectionManager) {
        this.axonServerConnectionManager = axonServerConnectionManager;
    }

    public CompletableFuture<Void> createTenant(String tenantName,
                                                String replicationGroup,
                                                boolean initializeSchema) {
        AxonServerConnection admin = axonServerConnectionManager.getConnection("_admin");
        CreateContextRequest createContextRequest = CreateContextRequest.newBuilder()
                                                         .setName(tenantName)
                                                         .setReplicationGroupName(replicationGroup)
                                                         .build();

        return CompletableFuture.completedFuture(null)
                .thenRun(()-> {
                            if (initializeSchema) {
                                new EmbeddedDatabaseBuilder()
                                        .setType(EmbeddedDatabaseType.H2)
                                        .setName(tenantName+";INIT=create " +
                                                         "schema if not exists " +
                                                         "schema_a\\;create schema if not exists schema_b;" +
                                                         "DB_CLOSE_DELAY=-1;")
                                        .addScript("db/migration/V0.sql")
                                        .build();
        }
                })
                .thenCompose(s->admin.adminChannel().createContext(createContextRequest));
    }

}
