package io.axoniq.dev.samples.shipment.api;

import io.axoniq.dev.samples.uuid.ShipmentId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class CancelShipmentCommand {

    @TargetAggregateIdentifier
    ShipmentId shipmentId;

    public CancelShipmentCommand(ShipmentId shipmentId) {
        this.shipmentId = shipmentId;
    }

    public ShipmentId getShipmentId() {
        return shipmentId;
    }
}
