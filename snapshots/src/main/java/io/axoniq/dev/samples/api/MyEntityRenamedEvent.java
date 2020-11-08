package io.axoniq.dev.samples.api;

/**
 * @author Sara Pellegrini
 */
public class MyEntityRenamedEvent {

    private final String entityId;

    private final String name;

    public MyEntityRenamedEvent(String entityId, String name) {
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
