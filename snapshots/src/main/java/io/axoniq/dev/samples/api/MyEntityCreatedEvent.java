package io.axoniq.dev.samples.api;

/**
 * @author Sara Pellegrini
 */
public class MyEntityCreatedEvent {

    private final String entityId;

    private final String name;

    public MyEntityCreatedEvent(String entityId, String name) {
        this.entityId = entityId;
        this.name = name;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }
}
