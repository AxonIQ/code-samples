package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * @author Sara Pellegrini
 * @author Stefan Dragisic
 */
public class CreateMyEntityCommand {

    @TargetAggregateIdentifier
    private final String entityId;

    public CreateMyEntityCommand(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityId() {
        return entityId;
    }
}
