package com.deliverytracking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequest {
    private String trackingId;
    private String reason;        // CUSTOMER_ABSENT, ADDRESS_ISSUE, CUSTOMER_REQUEST
    private String notes;         // Optional extra notes
    private String newAddress;    // For ADDRESS_ISSUE
    private String newDeliveryDate; // To capture the date from your UI
}