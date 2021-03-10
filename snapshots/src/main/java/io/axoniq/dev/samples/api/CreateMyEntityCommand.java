package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * @author Sara Pellegrini
 */
public class CreateMyEntityCommand {

    @TargetAggregateIdentifier
    private final String entityId;

    private final String name;

    public CreateMyEntityCommand(String entityId, String name) {
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