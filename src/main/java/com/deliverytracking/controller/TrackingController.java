package com.deliverytracking.controller;

import com.deliverytracking.dto.ApiResponse;
import com.deliverytracking.dto.TrackingResponse;
import com.deliverytracking.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
@Tag(name = "Public Shipment Tracking", description = "Public endpoint for customers to track shipments — no authentication required")
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping("/{trackingId}")
    @Operation(
            summary = "Track a shipment",
            description = "Public endpoint. Enter your tracking ID (e.g. TRK-ABCD1234) to view shipment details and full status history. No login required."
    )
    public ResponseEntity<ApiResponse<TrackingResponse>> trackShipment(@PathVariable String trackingId) {
        TrackingResponse response = trackingService.publicTrack(trackingId);
        return ResponseEntity.ok(ApiResponse.success("Shipment tracking details retrieved", response));
    }
}
