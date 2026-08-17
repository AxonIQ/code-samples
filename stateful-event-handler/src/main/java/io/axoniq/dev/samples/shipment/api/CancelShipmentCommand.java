package io.axoniq.dev.samples.shipment.api;

import io.axoniq.dev.samples.uuid.ShipmentId;
import org.axonframework.modelling.annotation.TargetEntityId;

public record CancelShipmentCommand(
        @TargetEntityId ShipmentId shipmentId
) {

}
