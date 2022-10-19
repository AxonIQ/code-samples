package io.axoniq.dev.samples.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * @author Sara Pellegrini
 * @author Stefan Dragisic
 */
public class ValidateMyEntityCommand {

    @TargetAggregateIdentifier
    private final String entityId;

    private final String email;

    public ValidateMyEntityCommand(String entityId,
                                   String email) {
        this.entityId = entityId;
        this.email = email;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getEmail() {
        return email;
    }
}
