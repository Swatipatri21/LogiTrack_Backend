package com.deliverytracking.dto;

import com.deliverytracking.enums.ShipmentRouteStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class HubStepUpdateRequest {

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    @NotNull(message = "Step order is required")
    private Integer stepOrder;

    @NotNull(message = "Status is required")
    private ShipmentRouteStatus status; 
    
    private String failureReason;


}
