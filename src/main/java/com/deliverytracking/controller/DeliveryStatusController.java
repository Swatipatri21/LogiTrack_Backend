package com.deliverytracking.controller;

import com.deliverytracking.dto.ApiResponse;
import com.deliverytracking.dto.DelayReportRequest;
import com.deliverytracking.dto.DeliveryDateResponse;
import com.deliverytracking.dto.HubStepUpdateRequest;
import com.deliverytracking.dto.OtpConfirmRequest;
import com.deliverytracking.dto.RescheduleRequest;
import com.deliverytracking.dto.ShipmentRouteResponse;
import com.deliverytracking.dto.StatusUpdateRequest;
import com.deliverytracking.dto.StatusUpdateResponse;
import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.ShipmentRoute;
import com.deliverytracking.entity.User;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.UserRepository;
import com.deliverytracking.service.DeliveryDateService;
import com.deliverytracking.service.DeliveryStatusService;
import com.deliverytracking.service.OtpService;
import com.deliverytracking.service.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery Status Management", description = "Endpoints for updating delivery statuses and viewing timelines (ADMIN, STAFF only)")

public class DeliveryStatusController {

    private final DeliveryStatusService deliveryStatusService;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final ShipmentService shipmentService;
    private final DeliveryDateService deliveryDateService;

    // ─────────────────────────────────────────────
    // OLD ENDPOINTS (kept for admin manual override)
    // ─────────────────────────────────────────────

    @PostMapping("/update-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_MANAGER','STAFF')")
    @Operation(
        summary = "Manual status update (admin override)",
        description = "Directly update shipment status. For normal flow use /hub/update-step instead."
    )
    public ResponseEntity<ApiResponse<StatusUpdateResponse>> updateStatus(
            @Valid @RequestBody StatusUpdateRequest request) {
        StatusUpdateResponse response = deliveryStatusService.updateStatus(request);
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated successfully", response));
    }

    @GetMapping("/timeline/{trackingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary = "Get full status timeline",
        description = "Retrieves all status updates for a shipment in descending order (latest first)."
    )
    public ResponseEntity<ApiResponse<List<StatusUpdateResponse>>> getStatusTimeline(
            @PathVariable String trackingId) {
        List<StatusUpdateResponse> timeline = deliveryStatusService.getStatusTimeline(trackingId);
        return ResponseEntity.ok(ApiResponse.success("Status timeline retrieved successfully", timeline));
    }

    // ─────────────────────────────────────────────
    // NEW ENDPOINTS — hub routing flow
    // ─────────────────────────────────────────────

    @PutMapping("/hub/update-step")
    @PreAuthorize("hasAnyRole('ADMIN','HUB_MANAGER', 'STAFF')")
    @Operation(
        summary = "Update hub step status",
        description = "Staff updates the status of their hub's step for a shipment. " +
                      "Valid transitions: PENDING → ARRIVED → DISPATCHED. " +
                      "On last step ARRIVED, OTP is auto-generated and sent to customer. " +
                      "On DISPATCHED, next hub step is automatically unlocked."
    )
    public ResponseEntity<ApiResponse<String>> updateHubStep(
            @Valid @RequestBody HubStepUpdateRequest request,
            Authentication authentication) {

        User staff = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        deliveryStatusService.updateHubStatus(request, staff);

        return ResponseEntity.ok(ApiResponse.success(
            "Step updated to " + request.getStatus(), null));
    }

    @GetMapping("/hub/my-tasks")
    public ResponseEntity<List<ShipmentRouteResponse>> getMyHubTasks(Authentication authentication) {
        User staff = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<ShipmentRouteResponse> tasks = deliveryStatusService.getTasksForHub(staff);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/confirm-otp")
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_MANAGER','STAFF')")
    @Operation(
        summary = "Confirm delivery with OTP",
        description = "Staff enters the OTP provided by the customer to confirm delivery. " +
                      "OTP is valid for 15 minutes. Maximum 3 attempts before lockout."
    )
    public ResponseEntity<ApiResponse<String>> confirmDelivery(
            @Valid @RequestBody OtpConfirmRequest request,
            Authentication authentication) {

        User staff = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        otpService.confirmDelivery(request, staff);

        return ResponseEntity.ok(ApiResponse.success(
            "Delivery confirmed successfully", null));
    }

    @PostMapping("/resend-otp")
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_MANAGER','STAFF')")
    @Operation(
        summary = "Resend OTP to customer",
        description = "Generates a new OTP and sends it to the customer. " +
                      "Use when OTP has expired or customer did not receive it."
    )
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @RequestParam String trackingId) {

        otpService.resendOtp(trackingId);

        return ResponseEntity.ok(ApiResponse.success(
            "New OTP sent to customer", null));
    }
    
 // GET — customer checks expected delivery date
    @GetMapping("/expected-date/{trackingId}")
    @Operation(summary = "Get expected delivery date")
    public ResponseEntity<ApiResponse<DeliveryDateResponse>> getExpectedDate(
            @PathVariable String trackingId) {

        DeliveryDateResponse response = shipmentService.getDeliveryDate(trackingId);

        return ResponseEntity.ok(ApiResponse.success(
            "Delivery date retrieved", response));
    }

    // POST — admin manually reports a delay with reason
    @PostMapping("/report-delay")
    @PreAuthorize("hasAnyRole('ADMIN','HUB_MANAGER', 'STAFF')")
    @Operation(summary = "Report a delay manually")
    public ResponseEntity<ApiResponse<String>> reportDelay(
            @RequestBody DelayReportRequest request) {

        deliveryDateService.reportDelay(
            request.getTrackingId(),
            request.getReason(),
            request.getAdditionalHours()
        );

        return ResponseEntity.ok(ApiResponse.success(
            "Delay reported and revised date updated", null));
    }
    
 // PUT /api/delivery/reschedule
 // Called by Pune hub staff — not admin
// @PutMapping("/reschedule")
// @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
// @Operation(
//     summary = "Reschedule delivery after failed attempt",
//     description = "Pune hub staff reschedules delivery. " +
//                   "Generates new OTP and resets step to OUT_FOR_DELIVERY."
// )
// public ResponseEntity<ApiResponse<String>> rescheduleDelivery(
//         @RequestParam String trackingId,
//         Authentication authentication) {
//
//     User staff = userRepository.findByEmail(authentication.getName())
//         .orElseThrow(() -> new RuntimeException("User not found"));
//
//     deliveryStatusService.rescheduleDelivery(trackingId, staff);
//
//     return ResponseEntity.ok(ApiResponse.success(
//         "Delivery rescheduled. New OTP sent to customer.", null));
// }
    
    @PutMapping("/reschedule")
    @PreAuthorize("hasAnyRole('STAFF', 'HUB_MANAGER')")
    @Operation(
        summary = "Reschedule a failed delivery",
        description = "Resets last hub step to ARRIVED and sends a new OTP. " +
                      "Reasons: CUSTOMER_ABSENT, ADDRESS_ISSUE, CUSTOMER_REQUEST. " +
                      "For ADDRESS_ISSUE, provide newAddress to update the receiver address."
    )
    public ResponseEntity<ApiResponse<String>> rescheduleDelivery(
            @RequestBody RescheduleRequest request) {
     
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User staff = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
     
        deliveryStatusService.rescheduleDelivery(request, staff);
     
        return ResponseEntity.ok(ApiResponse.success(
            "Delivery rescheduled successfully. New OTP sent to customer.", null));
    }
}