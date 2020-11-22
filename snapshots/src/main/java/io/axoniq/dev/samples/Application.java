package io.axoniq.dev.samples;

import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author Sara Pellegrini
 * @author Lucas Campos
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * This {@link SnapshotTriggerDefinition} is responsible to trigger the Snapshot. In this implementation, it creates
     * a Snapshot every 5 events, including the Snapshot itself. eg: when we hit the 5th event, a snapshot will be
     * created (5 events). When we hit the 9th event, another snapshot will be created (1 snapshot event + 4 events) and
     * so on.
     *
     * @param snapshotter The default {@link Snapshotter} provided by Axon.
     * @return the configured {@link SnapshotTriggerDefinition}.
     */
    @Bean
    public SnapshotTriggerDefinition mySnapshotTriggerDefinition(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, 5);
    }
}
