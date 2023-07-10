package io.axoniq.multitenancy;

import org.axonframework.extensions.multitenancy.components.TenantConnectPredicate;
import org.axonframework.extensions.multitenancy.components.TenantDescriptor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class MultiTenantConfig {

    @Bean
    public TenantConnectPredicate tenantFilter() {
        return context -> context.tenantId().startsWith("tenant-");
    }

    @Bean
    public Function<TenantDescriptor, DataSourceProperties> tenantDataSourceResolver() {
        return tenant -> {
            DataSourceProperties properties = new DataSourceProperties();
            properties.setUrl("jdbc:h2:mem:" + tenant.tenantId());
            properties.setDriverClassName("org.h2.Driver");
            properties.setUsername("sa");
            return properties;
        };
    }
}

