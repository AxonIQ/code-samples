package io.axoniq.dev.samples.api;

/**
 * @author Sara Pellegrini
 */
public class GetMyEntityByIdQuery {

    private final String entityId;

    public GetMyEntityByIdQuery(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityId() {
        return entityId;
    }
}
