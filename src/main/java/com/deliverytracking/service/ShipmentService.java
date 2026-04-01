package com.deliverytracking.service;

import com.deliverytracking.dto.DeliveryDateResponse;
import com.deliverytracking.dto.ShipmentRequest;
import com.deliverytracking.dto.ShipmentResponse;
import com.deliverytracking.entity.*;
import com.deliverytracking.enums.ShipmentRouteStatus;
import com.deliverytracking.enums.ShipmentStatus;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final DeliveryStatusUpdateRepository statusUpdateRepository;
    private final GeoRoutingService geoRoutingService;
    private final ShipmentRouteRepository shipmentRouteRepository;
    private final DeliveryDateService deliveryDateService;
    private final EmailService emailService;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

//    @Transactional
//    public ShipmentResponse createShipment(ShipmentRequest request) {
//
//        // 1. Get current user
//        String email = getCurrentUserEmail();
//        User currentUser = userRepository.findByEmail(email)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                    "User not found: " + email));
//
//        // 2. Geocode addresses — catch errors before saving anything
//        double[] originCoords = geoRoutingService.geocodeAddress(request.getOrigin());
//        double[] destCoords   = geoRoutingService.geocodeAddress(request.getDestination());
//
//        // 3. Build shipment entity
//        Shipment shipment = Shipment.builder()
//                .trackingId(generateUniqueTrackingId())
//                .senderName(request.getSenderName())
//                .senderAddress(request.getSenderAddress())
//                .receiverName(request.getReceiverName())
//                .receiverAddress(request.getReceiverAddress())
//                .receiverPhone(request.getReceiverPhone())
//                .receiverEmail(request.getReceiverEmail())
//                .origin(request.getOrigin())
//                .destination(request.getDestination())
//                .weight(request.getWeight())
//                .description(request.getDescription())
//                .originLat(originCoords[0])
//                .originLng(originCoords[1])
//                .destinationLat(destCoords[0])
//                .destinationLng(destCoords[1])
//                .currentStatus(ShipmentStatus.CREATED)
//                .createdBy(currentUser)
//                .build();
//
//        shipment = shipmentRepository.save(shipment);
//
//        // 4. Build routing steps
//        Hub originHub      = geoRoutingService.findNearestHub(originCoords[0], originCoords[1]);
//        Hub destinationHub = geoRoutingService.findNearestHub(destCoords[0], destCoords[1]);
//        List<Hub> route    = geoRoutingService.buildHubRoute(originHub, destinationHub);
//
//        for (int i = 0; i < route.size(); i++) {
//            ShipmentRoute step = ShipmentRoute.builder()
//                    .shipment(shipment)
//                    .hub(route.get(i))
//                    .stepOrder(i)
//                    .isUnlocked(i == 0)
//                    .status(i == 0
//                        ? ShipmentRouteStatus.PENDING.name()
//                        : ShipmentRouteStatus.LOCKED.name())
//                    .build();
//            shipmentRouteRepository.save(step);
//        }
//
//        // 5. Calculate and set expected delivery date
//        LocalDateTime expectedDate = deliveryDateService
//            .calculateExpectedDeliveryDate(route.size());
//        shipment.setExpectedDeliveryDate(expectedDate);
//        shipmentRepository.save(shipment);
//
//        // 6. Log creation in DeliveryHistory
//        DeliveryHistory history = DeliveryHistory.builder()
//                .shipment(shipment)
//                .action("SHIPMENT_CREATED")
//                .performedBy(currentUser.getEmail())
//                .details("Shipment created with " + route.size()
//                    + " routing steps. Expected delivery: " + expectedDate)
//                .build();
//        deliveryHistoryRepository.save(history);
//
//        // 7. Save CREATED status in timeline
//        DeliveryStatusUpdate createdUpdate = new DeliveryStatusUpdate();
//        createdUpdate.setShipment(shipment);
//        createdUpdate.setStatus(ShipmentStatus.CREATED);
//        createdUpdate.setUpdatedBy(currentUser);
//        createdUpdate.setUpdatedAt(LocalDateTime.now());
//        createdUpdate.setLocation(request.getOrigin());
//        createdUpdate.setRemarks("Shipment registered. Package to be picked up from "
//            + request.getSenderName() + ", " + request.getSenderAddress());
//        statusUpdateRepository.save(createdUpdate);
//
//        // 8. Save DISPATCHED (picked up from sender) in timeline
//        DeliveryStatusUpdate pickedUpUpdate = new DeliveryStatusUpdate();
//        pickedUpUpdate.setShipment(shipment);
//        pickedUpUpdate.setStatus(ShipmentStatus.DISPATCHED);
//        pickedUpUpdate.setUpdatedBy(currentUser);
//        pickedUpUpdate.setUpdatedAt(LocalDateTime.now().plusMinutes(1));
//        pickedUpUpdate.setLocation(request.getSenderAddress());
//        pickedUpUpdate.setRemarks("Package picked up from sender — "
//            + request.getSenderName() + ", " + request.getSenderAddress()
//            + ". Heading to " + request.getOrigin() + " hub.");
//        statusUpdateRepository.save(pickedUpUpdate);
//
//        log.info("Shipment {} created. Expected delivery: {}",
//            shipment.getTrackingId(), expectedDate);
//
//        return mapToResponse(shipment);
//    }

    
    
//    @Transactional
    @Transactional
    public ShipmentResponse createShipment(ShipmentRequest request) {

        // 1. Get current user
        String email = getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // 2. Geocode addresses
        double[] originCoords = geoRoutingService.geocodeAddress(request.getOrigin());
        double[] destCoords = geoRoutingService.geocodeAddress(request.getDestination());

        // Null-check geocoding results
        if (originCoords == null) {
            throw new IllegalArgumentException("Could not geocode origin address: " + request.getOrigin());
        }
        if (destCoords == null) {
            throw new IllegalArgumentException("Could not geocode destination address: " + request.getDestination());
        }

        // 3. Build shipment entity
        Shipment shipment = Shipment.builder()
                .trackingId(generateUniqueTrackingId())
                .senderName(request.getSenderName())
                .senderPhone(request.getSenderPhone())       // ← was missing
                .senderEmail(request.getSenderEmail())
                .senderAddress(request.getSenderAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .receiverEmail(request.getReceiverEmail())
                .receiverAddress(request.getReceiverAddress())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .weight(request.getWeight())
                .description(request.getDescription())
                .originLat(originCoords[0])
                .originLng(originCoords[1])
                .destinationLat(destCoords[0])
                .destinationLng(destCoords[1])
                .currentStatus(ShipmentStatus.CREATED)
                .createdBy(currentUser)
                .build();

        // 4. Save and use the returned managed entity consistently
        Shipment saved = shipmentRepository.save(shipment);

        // 5. Build routing steps
        Hub originHub = geoRoutingService.findNearestHub(originCoords[0], originCoords[1]);
        Hub destinationHub = geoRoutingService.findNearestHub(destCoords[0], destCoords[1]);

        if (originHub == null) {
            throw new IllegalArgumentException("No hub found near origin: " + request.getOrigin());
        }
        if (destinationHub == null) {
            throw new IllegalArgumentException("No hub found near destination: " + request.getDestination());
        }

        List<Hub> route = geoRoutingService.buildHubRoute(originHub, destinationHub);

        for (int i = 0; i < route.size(); i++) {
            ShipmentRoute step = ShipmentRoute.builder()
                    .shipment(saved)                          // ← use `saved`, not `shipment`
                    .hub(route.get(i))
                    .stepOrder(i)
                    .isUnlocked(i == 0)
                    .status(i == 0
                        ? ShipmentRouteStatus.PENDING.name()
                        : ShipmentRouteStatus.LOCKED.name())
                    .build();
            shipmentRouteRepository.save(step);
        }

        // 6. Calculate expected delivery date
//        LocalDateTime expectedDate = deliveryDateService.calculateExpectedDeliveryDate(route.size());
//        saved.setExpectedDeliveryDate(expectedDate);
        
        LocalDateTime expectedDate = deliveryDateService.calculateExpectedDeliveryDate(
        	    originCoords[0], originCoords[1],   // origin lat/lng
        	    destCoords[0],   destCoords[1],     // destination lat/lng
        	    route.size()                         // hub count for processing overhead
        	);
         saved.setExpectedDeliveryDate(expectedDate);
        saved = shipmentRepository.save(saved); 
        // 7. Log creation in DeliveryHistory
        DeliveryHistory history = DeliveryHistory.builder()
                .shipment(saved)                              // ← use `saved`
                .action("SHIPMENT_CREATED")
                .performedBy(currentUser.getEmail())
                .details("Shipment created with " + route.size() + " steps. Expected: " + expectedDate)
                .build();
        deliveryHistoryRepository.save(history);

        // 8. Save CREATED status in timeline
        DeliveryStatusUpdate createdUpdate = new DeliveryStatusUpdate();
        createdUpdate.setShipment(saved);                     // ← use `saved`
        createdUpdate.setStatus(ShipmentStatus.CREATED);
        createdUpdate.setUpdatedBy(currentUser);
        createdUpdate.setUpdatedAt(LocalDateTime.now());
        createdUpdate.setLocation(request.getOrigin());
        createdUpdate.setRemarks("Shipment registered. Package to be picked up from " + request.getSenderName());
        statusUpdateRepository.save(createdUpdate);

        // 9. Transition to DISPATCHED so first hub can act on it
        saved.setCurrentStatus(ShipmentStatus.DISPATCHED);
        Shipment dispatched = shipmentRepository.save(saved); // ← save and reassign

        DeliveryStatusUpdate pickedUpUpdate = new DeliveryStatusUpdate();
        pickedUpUpdate.setShipment(dispatched);               // ← use `dispatched`
        pickedUpUpdate.setStatus(ShipmentStatus.DISPATCHED);
        pickedUpUpdate.setUpdatedBy(currentUser);
        pickedUpUpdate.setUpdatedAt(LocalDateTime.now().plusMinutes(1));
        pickedUpUpdate.setLocation(request.getSenderAddress());
        pickedUpUpdate.setRemarks("Package picked up from sender. Heading to " + route.get(0).getName());
        statusUpdateRepository.save(pickedUpUpdate);

        log.info("Shipment {} created and dispatched to first hub.", dispatched.getTrackingId());

        // 10. Send emails
        emailService.sendShipmentCreatedEmail(saved);
        emailService.sendShipmentReceiverEmail(saved);

        return mapToResponse(dispatched);
    }
    // ─────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────

    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ShipmentResponse getShipmentByTrackingId(String trackingId) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Shipment not found: " + trackingId));
        return mapToResponse(shipment);
    }

    public List<ShipmentResponse> getShipmentsByStatus(ShipmentStatus status) {
        return shipmentRepository.findAllByCurrentStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    @Transactional
    public void deleteShipment(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Shipment not found: " + id));

        deliveryHistoryRepository.deleteAll(
            deliveryHistoryRepository.findAllByShipmentOrderByTimestampDesc(shipment));

        statusUpdateRepository.deleteAll(
            statusUpdateRepository.findAllByShipmentOrderByUpdatedAtDesc(shipment));

        // Delete shipment routes first — foreign key constraint
        List<ShipmentRoute> routes = shipmentRouteRepository
            .findByShipmentIdOrderByStepOrder(shipment.getId());
        shipmentRouteRepository.deleteAll(routes);

        shipmentRepository.delete(shipment);

        log.info("Shipment {} deleted.", shipment.getTrackingId());
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    ShipmentResponse mapToResponse(Shipment shipment) {

        // Build human readable message
        LocalDateTime referenceDate = shipment.getRevisedDeliveryDate() != null
            ? shipment.getRevisedDeliveryDate()
            : shipment.getExpectedDeliveryDate();

        String message = "Calculating...";
        if (referenceDate != null) {
            long daysLeft = java.time.Duration.between(
                LocalDateTime.now(), referenceDate).toDays();
            if (shipment.getCurrentStatus() == ShipmentStatus.DELIVERED) {
                message = "Delivered";
            } else if (daysLeft <= 0) {
                message = shipment.isDelayed() ? "Delayed — arriving today" : "Expected today";
            } else if (daysLeft == 1) {
                message = shipment.isDelayed() ? "Delayed — expected tomorrow" : "Expected tomorrow";
            } else {
                message = shipment.isDelayed()
                    ? "Delayed — expected in " + daysLeft + " days"
                    : "Expected in " + daysLeft + " days";
            }
        }

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingId(shipment.getTrackingId())
                .senderName(shipment.getSenderName())
                .senderAddress(shipment.getSenderAddress())
                .receiverName(shipment.getReceiverName())
                .receiverAddress(shipment.getReceiverAddress())
                .receiverPhone(shipment.getReceiverPhone())
                .receiverEmail(shipment.getReceiverEmail())
                .origin(shipment.getOrigin())
                .destination(shipment.getDestination())
                .weight(shipment.getWeight())
                .description(shipment.getDescription())
                .currentStatus(shipment.getCurrentStatus())
                .createdByEmail(shipment.getCreatedBy().getEmail())
                .expectedDeliveryDate(shipment.getExpectedDeliveryDate())
                .revisedDeliveryDate(shipment.getRevisedDeliveryDate())
                .isDelayed(shipment.isDelayed())
                .delayReason(shipment.getDelayReason())
                .estimatedDaysMessage(message)
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }

    private String generateUniqueTrackingId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String trackingId;
        do {
            StringBuilder sb = new StringBuilder("TRK-");
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            trackingId = sb.toString();
        } while (shipmentRepository.existsByTrackingId(trackingId));
        return trackingId;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
    
 // In ShipmentService.java — add this method
    public DeliveryDateResponse getDeliveryDate(String trackingId) {

        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Shipment not found: " + trackingId));

        // Build human readable message
        LocalDateTime referenceDate = shipment.getRevisedDeliveryDate() != null
            ? shipment.getRevisedDeliveryDate()
            : shipment.getExpectedDeliveryDate();

        String message;
        if (shipment.getCurrentStatus() == ShipmentStatus.DELIVERED) {
            message = "Delivered";
        } else if (referenceDate == null) {
            message = "Delivery date not yet calculated";
        } else {
            long daysLeft = java.time.Duration.between(
                LocalDateTime.now(), referenceDate).toDays();

            if (daysLeft <= 0) {
                message = shipment.isDelayed()
                    ? "Delayed — arriving today"
                    : "Expected today";
            } else if (daysLeft == 1) {
                message = shipment.isDelayed()
                    ? "Delayed — expected tomorrow"
                    : "Expected tomorrow";
            } else {
                message = shipment.isDelayed()
                    ? "Delayed — expected in " + daysLeft + " days"
                    : "Expected in " + daysLeft + " days";
            }
        }

        return DeliveryDateResponse.builder()
            .trackingId(trackingId)
            .expectedDeliveryDate(shipment.getExpectedDeliveryDate())
            .revisedDeliveryDate(shipment.getRevisedDeliveryDate())
            .isDelayed(shipment.isDelayed())
            .delayReason(shipment.getDelayReason())
            .estimatedDaysMessage(message)
            .build();
    }
}
