package com.deliverytracking.dto;

import com.deliverytracking.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    @NotBlank(message = "Tracking ID is required")
    private String trackingId;

    @NotNull(message = "Status is required")
    private String status;

    private String remarks;

    private String location;
    
    public ShipmentStatus getStatusAsEnum() {
        if (this.status == null || this.status.isBlank()) {
            throw new RuntimeException("Status cannot be empty.");
        }
        try {
            return ShipmentStatus.valueOf(this.status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                "Invalid status: '" + this.status + "'. Allowed values: "
                + java.util.Arrays.toString(ShipmentStatus.values()));
        }
    }
}
