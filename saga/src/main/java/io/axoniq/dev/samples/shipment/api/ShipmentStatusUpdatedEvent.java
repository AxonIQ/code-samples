package io.axoniq.dev.samples.shipment.api;

import io.axoniq.dev.samples.uuid.ShipmentId;

public class ShipmentStatusUpdatedEvent {

    ShipmentId shipmentId;

    ShipmentStatus shipmentStatus;

    public ShipmentStatusUpdatedEvent(ShipmentId shipmentId, ShipmentStatus shipmentStatus) {
        this.shipmentId = shipmentId;
        this.shipmentStatus = shipmentStatus;
    }

    public ShipmentId getShipmentId() {
        return shipmentId;
    }

    public ShipmentStatus getShipmentStatus() {
        return shipmentStatus;
    }
}
