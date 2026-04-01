package com.deliverytracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDateResponse {

    private String trackingId;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime revisedDeliveryDate;  // null if no delay
    private boolean isDelayed;
    private String delayReason;                 // null if no delay
    private String estimatedDaysMessage;        // human readable e.g. "Expected in 2 days"
}