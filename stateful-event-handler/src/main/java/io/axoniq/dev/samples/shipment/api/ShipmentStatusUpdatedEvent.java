package io.axoniq.dev.samples.shipment.api;


import io.axoniq.dev.samples.uuid.ShipmentId;

public record ShipmentStatusUpdatedEvent(
        ShipmentId shipmentId,
        ShipmentStatus shipmentStatus
) {

}