package io.axoniq.multitenancy;

import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.axonserver.connector.event.axon.AxonServerEventStore;
import org.axonframework.eventsourcing.snapshotting.SnapshotFilter;
import org.axonframework.extensions.multitenancy.components.TenantConnectPredicate;

import org.axonframework.extensions.multitenancy.configuration.MultiTenantStreamableMessageSourceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiTenantConfig {

    @Bean
    public TenantConnectPredicate tenantFilter() {
        return context -> context.tenantId().startsWith("tenant-");
    }

    @Bean
    public MultiTenantStreamableMessageSourceProvider multiTenantStreamableMessageSourceProvider(AxonServerEventStore defaultEventStore) {
        return (defaultTenantSource, processorName, tenantDescriptor, configuration) -> {
            if (tenantDescriptor.tenantId().startsWith("tenant-")) {
                return defaultTenantSource;
            }
            return defaultEventStore;

        };
    }

    @Bean
    public AxonServerEventStore defaultEventStore(AxonServerConfiguration configuration, AxonServerConnectionManager connectionManager) {
        return AxonServerEventStore.builder()
                .defaultContext("default")
                .configuration(configuration)
                .platformConnectionManager(connectionManager)
                .snapshotFilter(SnapshotFilter.allowAll())
                .build();
    }

    // UNCOMMENT THIS BEAN TO ENABLE MULTITENANCY WITH MULTIPLE DATA SOURCES
//    @Bean
//    public Function<TenantDescriptor, DataSourceProperties> tenantDataSourceResolver() {
//        return tenant -> {
//            DataSourceProperties properties = new DataSourceProperties();
//            properties.setUrl("jdbc:h2:mem:" + tenant.tenantId());
//            properties.setDriverClassName("org.h2.Driver");
//            properties.setUsername("sa");
//            return properties;
//        };
//    }
}

