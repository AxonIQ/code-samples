package io.axoniq.config;

import io.axoniq.axonserver.connector.admin.AdminChannel;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


/*
    Creates an admin channel from the configuration. used to simplify testing in other components.
 */
@Component
public class ConfigBasedAdminChannel {
    public ConfigBasedAdminChannel(AxonServerConnectionManager axonServerConnectionManager){
        this.axonServerConnectionManager = axonServerConnectionManager;
    }
    private final AxonServerConnectionManager axonServerConnectionManager;

    @Bean
    public AdminChannel adminChannel() {
        return axonServerConnectionManager.getConnection().adminChannel();
    }
}
