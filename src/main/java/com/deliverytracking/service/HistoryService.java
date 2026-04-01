package com.deliverytracking.service;

import com.deliverytracking.dto.HistoryResponse;
import com.deliverytracking.entity.DeliveryHistory;
import com.deliverytracking.entity.DeliveryStatusUpdate;
import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.ShipmentRoute;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.DeliveryHistoryRepository;
import com.deliverytracking.repository.DeliveryStatusUpdateRepository;
import com.deliverytracking.repository.HubRepository;
import com.deliverytracking.repository.ShipmentRepository;
import com.deliverytracking.repository.ShipmentRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final ShipmentRepository shipmentRepository;
    private final HubRepository hubRepository;
    private final ShipmentRouteRepository shipmentRouteRepository;
    private final DeliveryStatusUpdateRepository statusUpdateRepository;

    // ─── Public Methods ───────────────────────────────────────────────────────

    public List<HistoryResponse> getAllHistory() {
        return deliveryHistoryRepository.findAll()
                .stream()
                .map(h -> mapToResponse(h, null))
                .collect(Collectors.toList());
    }
    
    public List<HistoryResponse> getHubActivityLog(Long hubId) {
        return statusUpdateRepository.findByHubId(hubId)
            .stream()
            .map(this::mapStatusToResponse)
            .collect(Collectors.toList());
    }

    private HistoryResponse mapStatusToResponse(DeliveryStatusUpdate update) {
    	return HistoryResponse.builder()
    	        .id(update.getId())
    	        .trackingId(update.getShipment() != null ? update.getShipment().getTrackingId() : "N/A")
    	        .action(update.getStatus() != null ? update.getStatus().toString() : "UNKNOWN")
    	        .performedBy(update.getUpdatedBy() != null ? update.getUpdatedBy().getName() : "System")
    	        .timestamp(update.getUpdatedAt())
    	        .details(update.getRemarks())
    	        // 2. Access the Hub directly from the update entity
    	        .hubId(update.getHub() != null ? update.getHub().getId() : null)
    	        .hubName(update.getHub() != null ? update.getHub().getName() : update.getLocation())
    	        .build();
    }

    public List<HistoryResponse> getHistoryByShipment(String trackingId) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + trackingId));
        return deliveryHistoryRepository.findAllByShipmentOrderByTimestampDesc(shipment)
                .stream()
                .map(h -> mapToResponse(h, null))
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getHistoryByHub(Long hubId) {
        // Uses a single JOIN query — no N+1, no full table scan
        return deliveryHistoryRepository.findAllByHubId(hubId)
                .stream()
                .map(h -> mapToResponse(h, hubId))
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getHistoryByHubAndDateRange(Long hubId, LocalDateTime from, LocalDateTime to) {
        return deliveryHistoryRepository.findAllByHubIdAndDateRange(hubId, from, to)
                .stream()
                .map(h -> mapToResponse(h, hubId))
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getHistoryByDateRange(LocalDateTime from, LocalDateTime to) {
        return deliveryHistoryRepository.findAllByTimestampBetweenOrderByTimestampDesc(from, to)
                .stream()
                .map(h -> mapToResponse(h, null))
                .collect(Collectors.toList());
    }

    // ─── Private Mapper ───────────────────────────────────────────────────────

    /**
     * Maps a DeliveryHistory entity to a HistoryResponse DTO.
     *
     * @param history     the history record to map
     * @param targetHubId if non-null, resolves hub info for that specific hub
     *                    (used for hub-scoped queries so the correct hub name
     *                    is shown instead of always the first route hub).
     *                    Pass null for admin/global views — falls back to first hub in route.
     */
    private HistoryResponse mapToResponse(DeliveryHistory history, Long targetHubId) {
        Long resolvedHubId = null;
        String resolvedHubName = null;

        if (history.getShipment() != null) {
            List<ShipmentRoute> routes = shipmentRouteRepository.findByShipment(history.getShipment());

            if (routes != null && !routes.isEmpty()) {
                ShipmentRoute matched;

                if (targetHubId != null) {
                    // Hub-scoped: find the route entry for the specific hub being queried
                    matched = routes.stream()
                            .filter(r -> r.getHub() != null && targetHubId.equals(r.getHub().getId()))
                            .findFirst()
                            .orElse(null);
                } else {
                    // Global/admin view: just pick the first available hub in the route
                    matched = routes.stream()
                            .filter(r -> r.getHub() != null)
                            .findFirst()
                            .orElse(null);
                }

                if (matched != null && matched.getHub() != null) {
                    resolvedHubId   = matched.getHub().getId();
                    resolvedHubName = matched.getHub().getName();
                }
            }
        }

        return HistoryResponse.builder()
                .id(history.getId())
                .trackingId(history.getShipment().getTrackingId())
                .action(history.getAction())
                .performedBy(history.getPerformedBy())
                .timestamp(history.getTimestamp())
                .details(history.getDetails())
                .hubId(resolvedHubId)
                .hubName(resolvedHubName)
                .build();
    }
}