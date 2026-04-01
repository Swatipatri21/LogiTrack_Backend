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
public class HistoryResponse {

    private Long id;
    private String trackingId;
    private String action;
    private String performedBy;
    private LocalDateTime timestamp;
    private String details;
    private Long hubId;       // 👈 add
    private String hubName; 
}
