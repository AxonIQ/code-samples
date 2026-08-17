package io.axoniq.dev.samples.api;

import org.axonframework.modelling.annotation.TargetEntityId;

public record RenameMyEntityCommand(
        @TargetEntityId String entityId,
        String name
) {

}
