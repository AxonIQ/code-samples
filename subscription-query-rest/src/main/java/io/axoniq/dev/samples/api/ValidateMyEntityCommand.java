package io.axoniq.dev.samples.api;

import org.axonframework.modelling.annotation.TargetEntityId;

public record ValidateMyEntityCommand(
        @TargetEntityId String entityId, String email
) {

}
