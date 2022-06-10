package io.axoniq.config;

import io.axoniq.axonserver.connector.AxonServerConnectionFactory;
import io.axoniq.axonserver.connector.admin.AdminChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


/*
    Creates an admin channel from the configuration. used to simplify testing in other components.
 */
@Component
public class ConfigBasedAdminChannel {

    private final String contextName;
    private final String componentName;

    public ConfigBasedAdminChannel(@Value("${axon.axonserver.context}") String contextName,
                                   @Value("${axon.axonserver.component-name}") String componentName){
        this.contextName = contextName;
        this.componentName = componentName;
    }

    @Bean
    public AdminChannel adminChannel() {
        return AxonServerConnectionFactory.forClient(componentName)
                .build()
                .connect(contextName)
                .adminChannel();
    }
}
