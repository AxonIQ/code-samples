package io.axoniq.dev.samples;

import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class SnapshottingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnapshottingApplication.class, args);
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

    /**
     * Construct the main {@link Serializer} used by this sample application to be a {@link JacksonSerializer}.
     * <p>
     * By making this the {@link Primary} instance, Axon Framework will use this {@code Serializer} for commands,
     * events, queries, but more importantly, also snapshots. Since (by default) Axon Framework serializes the state of
     * the aggregate as the snapshot, this means the {@code io.axoniq.dev.samples.command.MyEntityAggregate} needs to be
     * serializable by Jackson.
     * <p>
     * To support Jackson de-/serialization for this projects aggregate, we added the
     * {@link com.fasterxml.jackson.annotation.JsonGetter} and {@link com.fasterxml.jackson.annotation.JsonSetter}
     * annotation to getter and setter methods. Note that the getters/setters are made package-private on purpose, as
     * they should only be used by the serializer.
     *
     * @return The main {@link Serializer} used by this sample application to be a {@link JacksonSerializer}.
     */
    @Bean
    @Primary
    public Serializer serializer() {
        return JacksonSerializer.defaultSerializer();
    }
}
