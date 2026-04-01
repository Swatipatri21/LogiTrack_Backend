package com.deliverytracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor 
public class DelayReportRequest {

    @NotBlank(message = "Tracking ID is required")
    private String trackingId;

    @NotBlank(message = "Reason is required")
    private String reason;
    // Examples:
    // "Vehicle breakdown near Wardha"
    // "Bad weather conditions"
    // "High volume at Ahmednagar hub"

    @NotNull(message = "Additional hours is required")
    private Integer additionalHours;
}