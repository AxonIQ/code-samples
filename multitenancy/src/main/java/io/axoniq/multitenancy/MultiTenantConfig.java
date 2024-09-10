package io.axoniq.multitenancy;

import org.axonframework.extensions.multitenancy.components.TenantConnectPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class MultiTenantConfig {

    @Bean
    public TenantConnectPredicate tenantFilter() {
        return context -> context.tenantId().startsWith("tenant-");
    }

    @Bean
    public ScheduledExecutorService persistentStreamScheduler() {
        return Executors.newScheduledThreadPool(10, Thread.ofVirtual()
                                                          .name("persistent-streams-", 0)
                                                          .factory());
    }

}

