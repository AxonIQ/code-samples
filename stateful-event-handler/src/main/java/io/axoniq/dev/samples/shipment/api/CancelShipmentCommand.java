package io.axoniq.dev.samples.shipment.api;

import io.axoniq.dev.samples.uuid.ShipmentId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record CancelShipmentCommand(
        @TargetAggregateIdentifier ShipmentId shipmentId
) {

}
