package com.deliverytracking.controller;

import com.deliverytracking.dto.ApiResponse;


import com.deliverytracking.dto.ShipmentRequest;
import com.deliverytracking.dto.ShipmentResponse;
import com.deliverytracking.dto.ShipmentRouteResponse;
import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.ShipmentRoute;
import com.deliverytracking.enums.ShipmentStatus;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.ShipmentRepository;
import com.deliverytracking.repository.ShipmentRouteRepository;
import com.deliverytracking.service.EmailService;
import com.deliverytracking.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipment Management", description = "Endpoints for creating and managing shipments (ADMIN, STAFF only)")
@SecurityRequirement(name = "bearerAuth")
public class ShipmentController {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentRouteRepository shipmentRouteRepository;
    private final ShipmentService shipmentService;
    
    private static final Logger logger =
            LoggerFactory.getLogger(EmailService.class);

    // ─────────────────────────────────────────────
    // SHIPMENT CRUD
    // ─────────────────────────────────────────────

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new shipment")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @Valid @RequestBody ShipmentRequest request) {
    	 logger.info("Received ShipmentRequest: {}", request);
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment created successfully", response));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getAllShipments() {
        List<ShipmentResponse> shipments = shipmentService.getAllShipments();
        return ResponseEntity.ok(ApiResponse.success(
            "Shipments retrieved successfully", shipments));
    }

    @GetMapping("/{trackingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get shipment by tracking ID")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentByTrackingId(
            @PathVariable String trackingId) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingId(trackingId);
        return ResponseEntity.ok(ApiResponse.success(
            "Shipment retrieved successfully", response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get shipments by status")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getShipmentsByStatus(
            @PathVariable ShipmentStatus status) {
        List<ShipmentResponse> shipments = shipmentService.getShipmentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(
            "Shipments filtered by status: " + status, shipments));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a shipment")
    public ResponseEntity<ApiResponse<Void>> deleteShipment(@PathVariable Long id) {
        shipmentService.deleteShipment(id);
        return ResponseEntity.ok(ApiResponse.success("Shipment deleted successfully", null));
    }

    

    @GetMapping("/{trackingId}/route")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary = "Get full hub route for a shipment",
        description = "Returns all hub steps for a shipment in order, " +
                      "showing which are locked, unlocked, and completed."
    )
    public ResponseEntity<ApiResponse<List<ShipmentRouteResponse>>> getShipmentRoute(
            @PathVariable String trackingId) {

        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Shipment not found: " + trackingId));

        List<ShipmentRoute> routes = shipmentRouteRepository
            .findByShipmentIdOrderByStepOrder(shipment.getId());

        List<ShipmentRouteResponse> response = routes.stream()
                .map(route -> ShipmentRouteResponse.builder()
                        .id(route.getId())
                        .shipmentTrackingId(trackingId)
                        .shipmentId(route.getShipment().getId())
                        .hubName(route.getHub().getName())
                        .hubCity(route.getHub().getCity())
                        .hubLat(route.getHub().getLatitude())   // ← directly from hub
                        .hubLng(route.getHub().getLongitude())  // ← directly from hub
                        .stepOrder(route.getStepOrder())
                        .status(route.getStatus())
                        .isUnlocked(route.isUnlocked())
                        .updatedAt(route.getUpdatedAt())
                        .build()
                )
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
            "Route retrieved successfully", response));
    }
}