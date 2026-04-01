package com.deliverytracking.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentRouteResponse {
	private Long id;
	private String shipmentTrackingId;
	private Long shipmentId;
    private String hubName;
    private String hubCity;
    private int stepOrder;
    private int totalSteps;  
    private String nextHubName;  // null when this is the last hub
    private String nextHubCity;  // null when this is the last hub
    private Double hubLat;    // ← add this
    private Double hubLng; 
    private boolean isUnlocked;
    private String status;
    private LocalDateTime updatedAt;
}