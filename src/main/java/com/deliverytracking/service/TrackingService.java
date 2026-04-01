package com.deliverytracking.service;

import com.deliverytracking.dto.StatusUpdateResponse;
import com.deliverytracking.dto.TrackingResponse;
import com.deliverytracking.entity.DeliveryStatusUpdate;
import com.deliverytracking.entity.Shipment;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.DeliveryStatusUpdateRepository;
import com.deliverytracking.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final ShipmentRepository shipmentRepository;
    private final DeliveryStatusUpdateRepository statusUpdateRepository;
    private final ShipmentService shipmentService;

    public TrackingResponse publicTrack(String trackingId) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No shipment found with tracking ID: " + trackingId + ". Please check and try again."));

        List<DeliveryStatusUpdate> updates = statusUpdateRepository
                .findAllByShipmentOrderByUpdatedAtDesc(shipment);

        List<StatusUpdateResponse> timeline = updates.stream()
                .map(update -> StatusUpdateResponse.builder()
                        .id(update.getId())
                        .trackingId(update.getShipment().getTrackingId())
                        .status(update.getStatus())
                        .remarks(update.getRemarks())
                        .location(update.getLocation())
                        .updatedByEmail(
                            update.getUpdatedBy() != null
                                ? update.getUpdatedBy().getEmail()
                                : "System (Automated)"
                        )
                        .updatedAt(update.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return TrackingResponse.builder()
                .shipmentDetails(shipmentService.mapToResponse(shipment))
                .statusTimeline(timeline)
                .build();
    }
}
