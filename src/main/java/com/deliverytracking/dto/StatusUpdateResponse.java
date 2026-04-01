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
public class StatusUpdateResponse {

    private Long id;
    private String trackingId;
    private ShipmentStatus status;
    private String remarks;
    private String location;
    private String updatedByEmail;
    private LocalDateTime updatedAt;
}
