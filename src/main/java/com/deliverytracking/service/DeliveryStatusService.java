package com.deliverytracking.service;

import com.deliverytracking.dto.HubStepUpdateRequest;
import com.deliverytracking.dto.RescheduleRequest;
import com.deliverytracking.dto.ShipmentRouteResponse;
import com.deliverytracking.dto.StatusUpdateRequest;
import com.deliverytracking.dto.StatusUpdateResponse;
import com.deliverytracking.entity.DeliveryStatusUpdate;
import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.ShipmentRoute;
import com.deliverytracking.entity.User;
import com.deliverytracking.enums.ShipmentRouteStatus;
import com.deliverytracking.enums.ShipmentStatus;
import com.deliverytracking.exception.InvalidStatusTransitionException;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.DeliveryHistoryRepository;
import com.deliverytracking.repository.DeliveryStatusUpdateRepository;
import com.deliverytracking.repository.ShipmentRepository;
import com.deliverytracking.repository.ShipmentRouteRepository;
import com.deliverytracking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryStatusService {

	private final DeliveryStatusUpdateRepository statusUpdateRepository;
	private final ShipmentRepository shipmentRepository;
	private final UserRepository userRepository;
	private final DeliveryHistoryRepository deliveryHistoryRepository;
	private final ShipmentRouteRepository shipmentRouteRepository;
	private final OtpService otpService;
	private final DeliveryDateService deliveryDateService;

	// ─────────────────────────────────────────────
	// OLD — manual status update (admin override)
	// ─────────────────────────────────────────────

	@Transactional
	public StatusUpdateResponse updateStatus(StatusUpdateRequest request) {

		// Fetch shipment
		Shipment shipment = shipmentRepository.findByTrackingId(request.getTrackingId())
				.orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + request.getTrackingId()));

		// Fetch the staff/admin making the update
		String email = getCurrentUserEmail();
		User updatedBy = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

		// Convert String → enum safely (handles null, wrong case, invalid value)
		ShipmentStatus newStatus = request.getStatusAsEnum();

		// Save a DeliveryStatusUpdate history record
		// FIX: was update.setStatus(request.getStatus()) — String into enum field →
		// type mismatch
		// Now correctly passes the enum
		DeliveryStatusUpdate update = new DeliveryStatusUpdate();
		update.setShipment(shipment);
		update.setStatus(newStatus); // enum, not String
		update.setRemarks(request.getRemarks());
		update.setLocation(request.getLocation());
		update.setUpdatedBy(updatedBy);
		update.setHub(updatedBy.getHub());
		update.setUpdatedAt(LocalDateTime.now());
		statusUpdateRepository.save(update);

		// Update the shipment's current status
		// FIX: was commented out — now restored using safe enum conversion
		shipment.setCurrentStatus(newStatus);
		shipmentRepository.save(shipment);

		log.info("Manual status update: shipment {} → {} by {}", shipment.getTrackingId(), newStatus, email);

		return mapToResponse(update);
	}

	// ─────────────────────────────────────────────
	// OLD — get full status timeline
	// ─────────────────────────────────────────────

	public List<StatusUpdateResponse> getStatusTimeline(String trackingId) {

		// Verify shipment exists first
		Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
				.orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + trackingId));

		// Fetch all status updates for this shipment, latest first
		List<DeliveryStatusUpdate> updates = statusUpdateRepository
				.findByShipmentIdOrderByUpdatedAtDesc(shipment.getId());

//        System.out.println(updates);

		return updates.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	// ─────────────────────────────────────────────
	// NEW — hub step validation
	// ─────────────────────────────────────────────

	private void validateStepUpdateRequest(HubStepUpdateRequest request, User staff) {

		ShipmentRoute step = shipmentRouteRepository
				.findByShipmentIdAndStepOrder(request.getShipmentId(), request.getStepOrder())
				.orElseThrow(() -> new ResourceNotFoundException("No route step found for shipment "
						+ request.getShipmentId() + " at step " + request.getStepOrder()));

		if (!step.isUnlocked()) {
			throw new RuntimeException("This step is locked. Previous hub has not dispatched yet.");
		}

		if (staff.getHub() == null) {
			throw new RuntimeException("You are not assigned to any hub. Contact admin.");
		}

		if (!staff.getHub().getId().equals(step.getHub().getId())) {
			throw new AccessDeniedException("You are assigned to " + staff.getHub().getName()
					+ " but this step belongs to " + step.getHub().getName());
		}

		ShipmentRouteStatus currentStatus = ShipmentRouteStatus.valueOf(step.getStatus());
		validateStatusTransition(currentStatus, request.getStatus());
	}

//	private void validateStatusTransition(ShipmentRouteStatus current, ShipmentRouteStatus incoming) {
//		boolean valid = switch (current) {
//		case PENDING -> incoming == ShipmentRouteStatus.ARRIVED;
//		case ARRIVED -> incoming == ShipmentRouteStatus.DISPATCHED || incoming == ShipmentRouteStatus.DELIVERED;
//		default -> false;
//		};
//
//		if (!valid) {
//			throw new InvalidStatusTransitionException("Invalid status transition: " + current + " → " + incoming);
//		}
//	}

	private void validateStatusTransition(ShipmentRouteStatus current, ShipmentRouteStatus incoming) {
		boolean valid = switch (current) {
		case PENDING -> incoming == ShipmentRouteStatus.ARRIVED;
		case ARRIVED -> incoming == ShipmentRouteStatus.DISPATCHED || incoming == ShipmentRouteStatus.DELIVERED
				|| incoming == ShipmentRouteStatus.DELIVERY_ATTEMPTED // ADD
				|| incoming == ShipmentRouteStatus.REJECTED // ADD
				|| incoming == ShipmentRouteStatus.FAILED; // ADD

// After failed attempt — staff can try again or mark failed
		case DELIVERY_ATTEMPTED -> incoming == ShipmentRouteStatus.DELIVERY_ATTEMPTED
				|| incoming == ShipmentRouteStatus.DELIVERED || incoming == ShipmentRouteStatus.FAILED;

		default -> false;
		};

		if (!valid) {
			throw new InvalidStatusTransitionException("Invalid status transition: " + current + " → " + incoming);
		}
	}
	
	
	// In your DeliveryService.java — wherever hub step status is updated
	@Transactional
	public void updateHubStatus(HubStepUpdateRequest request, User staff) {

	    // 1. Validate — hub assignment, lock check, status transition
	    validateStepUpdateRequest(request, staff);

	    // 2. Fetch step
	    ShipmentRoute step = shipmentRouteRepository
	        .findByShipmentIdAndStepOrder(request.getShipmentId(), request.getStepOrder())
	        .orElseThrow(() -> new ResourceNotFoundException("Route step not found"));

	    // 3. Update step fields
	    step.setStatus(request.getStatus().name());
	    if (request.getStatus() == ShipmentRouteStatus.ARRIVED) {
	        step.setUnlocked(true);
	    }
	    step.setUpdatedAt(LocalDateTime.now());
	    step.setUpdatedByUserId(staff.getId());
	    shipmentRouteRepository.save(step);

	    // 4. Determine last step
	    List<ShipmentRoute> allSteps = shipmentRouteRepository
	        .findByShipmentIdOrderByStepOrder(request.getShipmentId());
	    boolean isLastStep = (request.getStepOrder() == allSteps.size() - 1);
	    Shipment shipment  = step.getShipment();

	    // 5. DISPATCHED — unlock next hub
	    if (request.getStatus() == ShipmentRouteStatus.DISPATCHED && !isLastStep) {
	        shipmentRouteRepository
	            .findByShipmentIdAndStepOrder(request.getShipmentId(), request.getStepOrder() + 1)
	            .ifPresent(next -> {
	                next.setUnlocked(true);
	                next.setStatus(ShipmentRouteStatus.PENDING.name());
	                next.setUpdatedAt(LocalDateTime.now());
	                shipmentRouteRepository.save(next);
	            });
	        shipment.setCurrentStatus(ShipmentStatus.IN_TRANSIT);
	        shipmentRepository.save(shipment);
	        saveStatusUpdate(shipment, "DISPATCHED", staff, step.getHub().getName(), null);
	    }

	    // 6. ARRIVED at non-last hub — mark IN_TRANSIT
	    else if (request.getStatus() == ShipmentRouteStatus.ARRIVED && !isLastStep) {
	        shipment.setCurrentStatus(ShipmentStatus.IN_TRANSIT);
	        shipmentRepository.save(shipment);
	        saveStatusUpdate(shipment, "ARRIVED", staff, step.getHub().getName(), null);
	    }

	    // 7. Last step — OTP, delivery attempts, RETURNED_TO_SENDER
	    else if (isLastStep) {
	        handleLastStepTransitions(step, shipment, request, staff);
	    }

	    // 8. Auto delay check on every hub update
	    deliveryDateService.checkAndHandleDelay(shipment, request.getStepOrder());

	    log.info("Hub step updated: shipment={} step={} status={} by={}",
	        shipment.getTrackingId(), request.getStepOrder(),
	        request.getStatus(), staff.getEmail());
	}


	
	
//	@Transactional
//	public void updateHubStatus(HubStepUpdateRequest request, User staff) {
//	    // 1 & 2. Validation and Fetching (Keep as is)
//	    validateStepUpdateRequest(request, staff);
//	    ShipmentRoute step = shipmentRouteRepository
//	            .findByShipmentIdAndStepOrder(request.getShipmentId(), request.getStepOrder())
//	            .orElseThrow(() -> new ResourceNotFoundException("Route step not found"));
//
//	    // 3. Status Update + Forced Unlock (Safety measure)
//	    step.setStatus(request.getStatus().name());
//	    if (request.getStatus() == ShipmentRouteStatus.ARRIVED) {
//	        step.setUnlocked(true); // Force unlock if arrived
//	    }
//	    step.setUpdatedAt(LocalDateTime.now());
//	    step.setUpdatedByUserId(staff.getId());
//	    shipmentRouteRepository.save(step);
//
//	    // 4. Determine if last step
//	    List<ShipmentRoute> allSteps = shipmentRouteRepository.findByShipmentIdOrderByStepOrder(request.getShipmentId());
//	    boolean isLastStep = (request.getStepOrder() == allSteps.size() - 1);
//	    Shipment shipment = step.getShipment();
//
//	    // 5. DISPATCHED → Unlock Next Hub (Cleaned up)
//	    if (request.getStatus() == ShipmentRouteStatus.DISPATCHED && !isLastStep) {
//	        shipmentRouteRepository.findByShipmentIdAndStepOrder(request.getShipmentId(), request.getStepOrder() + 1)
//	            .ifPresent(nextRoute -> {
//	                nextRoute.setUnlocked(true);
//	                nextRoute.setStatus(ShipmentRouteStatus.PENDING.name());
//	                shipmentRouteRepository.save(nextRoute);
//	            });
//	    }
//
//	    // 6. Terminal State Handling (Last Step Logic)
//	    if (isLastStep) {
//	        handleLastStepTransitions(step, shipment, request, staff);
//	    } else {
//	        // Only log for non-terminal steps here to avoid duplicates
//	        saveStatusUpdate(shipment, request.getStatus().name(), staff, step.getHub().getName(), null);
//	    }
//
//	    deliveryDateService.checkAndHandleDelay(shipment, request.getStepOrder());
//	}

	// ADD THIS METHOD
	private void saveStatusUpdate(Shipment shipment, String routeStatus, User updatedBy, String hubName, String customRemarks) {

		String remarks = (customRemarks != null && !customRemarks.isBlank()) 
		        ? customRemarks 
		        : switch (routeStatus) {
		            case "ARRIVED" -> "Package arrived at " + hubName;
		            case "DISPATCHED" -> "Package dispatched from " + hubName;
		            case "DELIVERED" -> "Package delivered to customer";
		            case "OUT_FOR_DELIVERY" -> "Package is out for delivery from " + hubName;
		            default -> "Status updated at " + hubName;
		        };

		ShipmentStatus shipmentStatus = ShipmentStatus.valueOf(mapRouteStatusToShipmentStatus(routeStatus));

		DeliveryStatusUpdate update = new DeliveryStatusUpdate();
		update.setShipment(shipment);
		update.setStatus(shipmentStatus);
		update.setUpdatedBy(updatedBy);
		update.setUpdatedAt(LocalDateTime.now());
		update.setLocation(hubName);
		
		update.setHub(updatedBy.getHub());
		update.setRemarks(remarks);
		statusUpdateRepository.save(update);
	}


	private String mapRouteStatusToShipmentStatus(String routeStatus) {
	    return switch (routeStatus) {
	        case "ARRIVED" -> ShipmentStatus.IN_TRANSIT.name();
	        case "DISPATCHED" -> ShipmentStatus.DISPATCHED.name();
	        case "DELIVERED" -> ShipmentStatus.DELIVERED.name();
	        case "OUT_FOR_DELIVERY" -> ShipmentStatus.OUT_FOR_DELIVERY.name();
	        case "DELIVERY_ATTEMPTED" -> ShipmentStatus.DELIVERY_ATTEMPTED.name();
	        case "FAILED" -> ShipmentStatus.FAILED.name();
	        default -> ShipmentStatus.IN_TRANSIT.name();
	    };
	}


	private String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getName();
	}


	private StatusUpdateResponse mapToResponse(DeliveryStatusUpdate update) {
	    return StatusUpdateResponse.builder()
	        .id(update.getId())
	        .trackingId(update.getShipment().getTrackingId())
	        .status(update.getStatus())
	        .remarks(update.getRemarks())
	        .location(update.getLocation())
	        // THIS IS THE CRITICAL FIX: 
	        // If the system (DeliveryDateService) created this, updatedBy is NULL.
	        .updatedByEmail(
	            update.getUpdatedBy() != null 
	                ? update.getUpdatedBy().getEmail() 
	                : "System (Automated)"
	        )
	        .updatedAt(update.getUpdatedAt())
	        .build();
	}
	
	
	private void handleLastStepTransitions(ShipmentRoute step, Shipment shipment, HubStepUpdateRequest request, User staff) {
	    ShipmentRouteStatus incomingStatus = request.getStatus();

	    // Case A: Package Arrived at Final Hub -> Set to OUT_FOR_DELIVERY & Send OTP
	    if (incomingStatus == ShipmentRouteStatus.ARRIVED) {
	        shipment.setCurrentStatus(ShipmentStatus.OUT_FOR_DELIVERY);
	        shipmentRepository.save(shipment);
	        otpService.generateAndSendOtp(shipment);
	        
	        saveStatusUpdate(shipment, "OUT_FOR_DELIVERY", staff, step.getHub().getName(), 
	            "Package arrived at " + step.getHub().getName() + ". Out for delivery.");
	    }

	    // Case B: Delivery Attempted (Fail)
	    else if (incomingStatus == ShipmentRouteStatus.DELIVERY_ATTEMPTED) {
	        shipment.setDeliveryAttempts(shipment.getDeliveryAttempts() + 1);
	        shipment.setLastAttemptAt(LocalDateTime.now());
	        
	        if (request.getFailureReason() != null) {
	            shipment.setAttemptFailureReason(request.getFailureReason());
	        }

	        if (shipment.getDeliveryAttempts() >= shipment.getMaxDeliveryAttempts()) {
	            shipment.setCurrentStatus(ShipmentStatus.RETURNED_TO_SENDER);
	            step.setStatus(ShipmentRouteStatus.FAILED.name());
	            saveStatusUpdate(shipment, "FAILED", staff, step.getHub().getName(), "Max attempts reached. Returning to sender.");
	        } else {
	            // Keep at ARRIVED status so staff can try again tomorrow
	            step.setStatus(ShipmentRouteStatus.ARRIVED.name());
	            saveStatusUpdate(shipment, "DELIVERY_ATTEMPTED", staff, step.getHub().getName(), 
	                "Attempt " + shipment.getDeliveryAttempts() + " failed. Reason: " + shipment.getAttemptFailureReason());
	        }
	        shipmentRepository.save(shipment);
	        shipmentRouteRepository.save(step);
	    }

	    // Case C: Delivered (Handled by your separate OTP confirmation controller usually)
	    else if (incomingStatus == ShipmentRouteStatus.DELIVERED) {
	        shipment.setCurrentStatus(ShipmentStatus.DELIVERED);
	        shipmentRepository.save(shipment);
		}
	}

	
	@Transactional
	public void rescheduleDelivery(RescheduleRequest request, User staff) {
	 
	    String trackingId = request.getTrackingId();
	    // Default to CUSTOMER_ABSENT if null
	    String reason     = request.getReason() != null ? request.getReason() : "CUSTOMER_ABSENT";
	    String notes      = request.getNotes();
	    String newAddress = request.getNewAddress();
	    String newDate    = request.getNewDeliveryDate();
	 
	    // 1. Fetch shipment
	    Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
	        .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + trackingId));
	 
	    // 2. Only allow reschedule if delivery was attempted or out for delivery
	    if (shipment.getCurrentStatus() != ShipmentStatus.DELIVERY_ATTEMPTED &&
	        shipment.getCurrentStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
	        throw new RuntimeException("Cannot reschedule. Current status: " + shipment.getCurrentStatus());
	    }
	 
	    // 3. Increment Delivery Attempts (CRITICAL FIX)
	    shipment.setDeliveryAttempts(shipment.getDeliveryAttempts() + 1);

	    // 4. Check max attempts not exceeded
	    if (shipment.getDeliveryAttempts() > shipment.getMaxDeliveryAttempts()) {
	        throw new RuntimeException("Maximum delivery attempts reached. Cannot reschedule.");
	    }

	    // 5. Verify Hub Authorization
	    List<ShipmentRoute> allSteps = shipmentRouteRepository.findByShipmentIdOrderByStepOrder(shipment.getId());
	    ShipmentRoute lastStep = allSteps.get(allSteps.size() - 1);
	 
	    if (staff.getHub() == null || !staff.getHub().getId().equals(lastStep.getHub().getId())) {
	        throw new AccessDeniedException("Only staff at " + lastStep.getHub().getName() + " can reschedule this.");
	    }
	 
	    // 6. Handle Updates (Address & Fail Reason)
	    if ("ADDRESS_ISSUE".equals(reason) && newAddress != null && !newAddress.isBlank()) {
	        shipment.setReceiverAddress(newAddress);
	    }
	 
	    shipment.setAttemptFailureReason(reason + (notes != null && !notes.isBlank() ? " — " + notes : ""));
	 
	    // 7. Reset Route & Shipment State
	    lastStep.setStatus(ShipmentRouteStatus.ARRIVED.name());
	    lastStep.setUpdatedAt(LocalDateTime.now());
	    shipmentRouteRepository.save(lastStep);
	 
	    shipment.setCurrentStatus(ShipmentStatus.OUT_FOR_DELIVERY);
	    shipment.setOtpHash(null);
	    shipment.setOtpExpiry(null);
	    shipment.setOtpAttempts(0);
	    shipmentRepository.save(shipment);
	 
	    // 8. Generate fresh OTP
	    otpService.generateAndSendOtp(shipment);
	 
	    // 9. Build Timeline Message
	    String reasonLabel = switch (reason) {
	        case "CUSTOMER_ABSENT"  -> "Customer was not available";
	        case "ADDRESS_ISSUE"    -> "Address issue (Updated: " + newAddress + ")";
	        case "CUSTOMER_REQUEST" -> "Customer requested reschedule";
	        default                 -> reason;
	    };
	 
	    int attemptsRemaining = shipment.getMaxDeliveryAttempts() - shipment.getDeliveryAttempts();
	 
	    String timelineMsg = String.format(
	        "Rescheduled by %s. Reason: %s. New Date: %s. Attempt %d/%d. %d left.",
	        staff.getName(), reasonLabel, 
	        (newDate != null ? newDate : "Next available slot"),
	        shipment.getDeliveryAttempts(), shipment.getMaxDeliveryAttempts(),
	        attemptsRemaining
	    );
	 
	    saveStatusUpdate(shipment, "OUT_FOR_DELIVERY", staff, lastStep.getHub().getName(), timelineMsg);
	}
	
//	@Transactional
//	public void rescheduleDelivery(RescheduleRequest request, User staff) {
//	 
//	    String trackingId = request.getTrackingId();
//	    String reason     = request.getReason() != null ? request.getReason() : "CUSTOMER_ABSENT";
//	    String notes      = request.getNotes();
//	    
//	    String newAddress = request.getNewAddress();
//	 
//	    // 1. Fetch shipment
//	    Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
//	        .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + trackingId));
//	 
//	    // 2. Only allow reschedule if delivery was attempted
//	    if (shipment.getCurrentStatus() != ShipmentStatus.DELIVERY_ATTEMPTED &&
//	        shipment.getCurrentStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
//	        throw new RuntimeException(
//	            "Cannot reschedule. Current status: " + shipment.getCurrentStatus());
//	    }
//	 
//	    // 3. Only staff at the last hub can reschedule
//	    List<ShipmentRoute> allSteps = shipmentRouteRepository
//	        .findByShipmentIdOrderByStepOrder(shipment.getId());
//	    ShipmentRoute lastStep = allSteps.get(allSteps.size() - 1);
//	 
//	    if (staff.getHub() == null || !staff.getHub().getId().equals(lastStep.getHub().getId())) {
//	        throw new AccessDeniedException(
//	            "Only staff at " + lastStep.getHub().getName() + " can reschedule this delivery.");
//	    }
//	 
//	    // 4. Check max attempts not exceeded
//	    if (shipment.getDeliveryAttempts() >= shipment.getMaxDeliveryAttempts()) {
//	        throw new RuntimeException(
//	            "Maximum delivery attempts (" + shipment.getMaxDeliveryAttempts()
//	            + ") reached. Cannot reschedule.");
//	    }
//	 
//	    // 5. Handle ADDRESS_ISSUE — update receiver address if provided
//	    if ("ADDRESS_ISSUE".equals(reason) && newAddress != null && !newAddress.isBlank()) {
//	        shipment.setReceiverAddress(newAddress);
//	        log.info("Address updated for {} → {}", trackingId, newAddress);
//	    }
//	 
//	    // 6. Save failure reason
//	    shipment.setAttemptFailureReason(reason
//	        + (notes != null && !notes.isBlank() ? " — " + notes : ""));
//	 
//	    // 7. Reset last step back to ARRIVED so staff can try again
//	    lastStep.setStatus(ShipmentRouteStatus.ARRIVED.name());
//	    lastStep.setUpdatedAt(LocalDateTime.now());
//	    shipmentRouteRepository.save(lastStep);
//	 
//	    // 8. Reset shipment status and OTP
//	    shipment.setCurrentStatus(ShipmentStatus.OUT_FOR_DELIVERY);
//	    shipment.setOtpHash(null);
//	    shipment.setOtpExpiry(null);
//	    shipment.setOtpAttempts(0);
//	    shipmentRepository.save(shipment);
//	 
//	    // 9. Generate fresh OTP for next attempt
//	    otpService.generateAndSendOtp(shipment);
//	 
//	    // 10. Build human-readable timeline message per reason
//	    String reasonLabel = switch (reason) {
//	        case "CUSTOMER_ABSENT"   -> "Customer was not available";
//	        case "ADDRESS_ISSUE"     -> "Address issue" +
//	            (newAddress != null && !newAddress.isBlank() ? " — updated to: " + newAddress : "");
//	        case "CUSTOMER_REQUEST"  -> "Customer requested reschedule";
//	        default                  -> reason;
//	    };
//	 
//	    int attemptsLeft = shipment.getMaxDeliveryAttempts() - shipment.getDeliveryAttempts();
//	 
//	    String timelineMsg = "Delivery rescheduled by " + staff.getName()
//	        + ". Reason: " + reasonLabel
//	        + (notes != null && !notes.isBlank() ? ". Notes: " + notes : "")
//	        + ". Attempt " + (shipment.getDeliveryAttempts() + 1)
//	        + " of " + shipment.getMaxDeliveryAttempts()
//	        + " (" + attemptsLeft + " remaining). New OTP sent.";
//	 
//	    saveStatusUpdate(shipment, "OUT_FOR_DELIVERY", staff,
//	        lastStep.getHub().getName(), timelineMsg);
//	 
//	    log.info("Delivery rescheduled for {} by {}. Reason: {}. Attempt {} of {}.",
//	        trackingId, staff.getEmail(), reason,
//	        shipment.getDeliveryAttempts() + 1,
//	        shipment.getMaxDeliveryAttempts());
//	}
	
//	public List<ShipmentRoute> getTasksForHub(User staff) {
//	    if (staff.getHub() == null) {
//	        throw new RuntimeException("You are not assigned to any hub.");
//	    }
//
//	    // Both STAFF and HUB_MANAGER see the same task list
//	    // Anyone at the hub can pick up and process any shipment
//	    return shipmentRouteRepository
//	        .findByHubIdAndIsUnlockedTrueAndStatusIn(
//	            staff.getHub().getId(),
//	            List.of(
//	                ShipmentRouteStatus.PENDING.name(),
//	                ShipmentRouteStatus.ARRIVED.name()
//	            )
//	        );
//	}
	
	public List<ShipmentRouteResponse> getTasksForHub(User staff) {
	    if (staff.getHub() == null) {
	        throw new RuntimeException("You are not assigned to any hub.");
	    }
	 
	    // Fetch only this hub's unlocked tasks (PENDING or ARRIVED)
	    List<ShipmentRoute> routes = shipmentRouteRepository
	        .findByHubIdAndIsUnlockedTrueAndStatusIn(
	            staff.getHub().getId(),
	            List.of(
	                ShipmentRouteStatus.PENDING.name(),
	                ShipmentRouteStatus.ARRIVED.name()
	            )
	        );
	    
	    
	 
	    // For each route, look up the total number of steps for that shipment
	    // so the frontend can compute isLastStep = (stepOrder == totalSteps - 1)
	    return routes.stream().map(route -> {
	        int totalSteps = shipmentRouteRepository
	            .countByShipmentId(route.getShipment().getId());
	        
	        String nextHubName = null;
	        String nextHubCity = null;
	        if (route.getStepOrder() < totalSteps - 1) {
	            nextHubName = shipmentRouteRepository
	                .findByShipmentIdAndStepOrder(route.getShipment().getId(), route.getStepOrder() + 1)
	                .map(next -> next.getHub().getName())
	                .orElse(null);
	            nextHubCity = shipmentRouteRepository
	                .findByShipmentIdAndStepOrder(route.getShipment().getId(), route.getStepOrder() + 1)
	                .map(next -> next.getHub().getCity())
	                .orElse(null);
	        }
	 
	        return ShipmentRouteResponse.builder()
	            .id(route.getId())
	            .shipmentTrackingId(route.getShipment().getTrackingId())
	            .shipmentId(route.getShipment().getId())
	            .hubName(route.getHub().getName())
	            .hubCity(route.getHub().getCity())
	            .stepOrder(route.getStepOrder())
	            .totalSteps(totalSteps)          // ← key field
	            .hubLat(route.getHub().getLatitude())
	            .hubLng(route.getHub().getLongitude())
	            .nextHubName(nextHubName)   // ← null if last hub
	            .nextHubCity(nextHubCity) 
	            .isUnlocked(route.isUnlocked())
	            .status(route.getStatus())
	            .updatedAt(route.getUpdatedAt())
	            .build();
	    }).collect(Collectors.toList());
	}
}
