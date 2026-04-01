package com.deliverytracking.dto;

import com.deliverytracking.enums.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {

    private Long id;
    private String trackingId;
    private String senderName;
    private String senderAddress;
    private String receiverName;
    private String receiverAddress;
    private String receiverPhone;
    private String receiverEmail;          // ADD — was missing
    private String origin;
    private String destination;
    private Double weight;
    private String description;
    private ShipmentStatus currentStatus;
    private String createdByEmail;

    // delivery date fields
    private LocalDateTime expectedDeliveryDate;   // change LocalDate → LocalDateTime
    private LocalDateTime revisedDeliveryDate;    // ADD — was missing
    private boolean isDelayed;                    // ADD — was missing
    private String delayReason;                   // ADD — was missing
    private String estimatedDaysMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}