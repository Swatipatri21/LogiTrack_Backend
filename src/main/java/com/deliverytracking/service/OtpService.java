package com.deliverytracking.service;

import com.deliverytracking.dto.OtpConfirmRequest;
import com.deliverytracking.entity.DeliveryStatusUpdate;
import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.ShipmentRoute;
import com.deliverytracking.entity.User;
import com.deliverytracking.enums.ShipmentRouteStatus;
import com.deliverytracking.enums.ShipmentStatus;
import com.deliverytracking.exception.ResourceNotFoundException;
import com.deliverytracking.repository.DeliveryStatusUpdateRepository;
import com.deliverytracking.repository.ShipmentRepository;
import com.deliverytracking.repository.ShipmentRouteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentRouteRepository shipmentRouteRepository;
    private final PasswordEncoder passwordEncoder;
//    private final NotificationService notificationService;
    private final EmailService emailService;
    
    private final DeliveryStatusUpdateRepository statusUpdateRepository;

    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final int MAX_OTP_ATTEMPTS   = 3;

    // ─────────────────────────────────────────────
    // GENERATE AND SEND
    // ─────────────────────────────────────────────

    @Transactional
    public void generateAndSendOtp(Shipment shipment) {

        // Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));

        // Print to console for testing — remove in production
        log.info("===== OTP FOR {} : {} =====", shipment.getTrackingId(), otp);

        // Hash before storing — never store plain OTP
        shipment.setOtpHash(passwordEncoder.encode(otp));
        shipment.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        shipment.setOtpVerified(false);
        shipment.setOtpAttempts(0);
        shipmentRepository.save(shipment);

        // Send to customer
        emailService.sendDeliveryOtp(
            shipment.getReceiverPhone(),
            shipment.getReceiverEmail(),
            otp,
            shipment.getTrackingId()
        );
    }

    // ─────────────────────────────────────────────
    // CONFIRM DELIVERY WITH OTP
    // ─────────────────────────────────────────────

    @Transactional
    public void confirmDelivery(OtpConfirmRequest request, User staff) {

        // 1. Fetch shipment
        Shipment shipment = shipmentRepository.findByTrackingId(request.getTrackingId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Shipment not found: " + request.getTrackingId()));

        // 2. Already delivered — reject
        if (shipment.isOtpVerified()) {
            throw new RuntimeException("This shipment has already been delivered.");
        }

        // 3. OTP not yet generated — last hub hasn't marked ARRIVED yet
        if (shipment.getOtpHash() == null) {
            throw new RuntimeException(
                "OTP has not been generated yet. " +
                "Last hub must mark the shipment ARRIVED first.");
        }

        // 4. Max attempts exceeded — force resend
        if (shipment.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new RuntimeException(
                "Maximum OTP attempts exceeded. " +
                "Please request a new OTP via /resend-otp.");
        }

        // 5. OTP expired — force resend
        if (LocalDateTime.now().isAfter(shipment.getOtpExpiry())) {
            throw new RuntimeException(
                "OTP has expired. Please request a new OTP via /resend-otp.");
        }

        // 6. Wrong OTP — increment attempt counter
        if (!passwordEncoder.matches(request.getOtp(), shipment.getOtpHash())) {
            shipment.setOtpAttempts(shipment.getOtpAttempts() + 1);
            shipmentRepository.save(shipment);

            int remaining = MAX_OTP_ATTEMPTS - shipment.getOtpAttempts();
            throw new RuntimeException(
                "Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        // 7. OTP is valid — mark shipment delivered
        shipment.setOtpVerified(true);
        shipment.setOtpHash(null);          // clear hash — no longer needed
        shipment.setOtpExpiry(null);
        shipment.setOtpAttempts(0);
        shipment.setCurrentStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());
        shipment.setDeliveredByStaffId(staff.getId());
        shipmentRepository.save(shipment);
        
       
        
        DeliveryStatusUpdate deliveredUpdate = new DeliveryStatusUpdate();
        deliveredUpdate.setShipment(shipment);
        deliveredUpdate.setStatus(ShipmentStatus.DELIVERED);
        deliveredUpdate.setUpdatedBy(staff);
        if (staff.getHub() != null) {
            deliveredUpdate.setHub(staff.getHub());
        }
//        deliveredUpdate.setHub(shipment.)
        deliveredUpdate.setUpdatedAt(LocalDateTime.now());
        deliveredUpdate.setLocation("Delivered to customer");
        deliveredUpdate.setRemarks("OTP verified. Delivered by " + staff.getName());
        statusUpdateRepository.save(deliveredUpdate);

        // 8. Mark final ShipmentRoute step as DELIVERED
        List<ShipmentRoute> allSteps = shipmentRouteRepository
            .findByShipmentIdOrderByStepOrder(shipment.getId());

        if (!allSteps.isEmpty()) {
            ShipmentRoute lastStep = allSteps.get(allSteps.size() - 1);
            lastStep.setStatus(ShipmentRouteStatus.DELIVERED.name());
            lastStep.setUpdatedAt(LocalDateTime.now());
            lastStep.setUpdatedByUserId(staff.getId());
            shipmentRouteRepository.save(lastStep);
        }

        // 9. Send delivery confirmation to customer
//        emailService.sendDeliveryOtpEmail(
//            shipment.getReceiverEmail(),
//            
//            shipment.getTrackingId(),
//            shipment.getDeliveredAt()
//        );
     // 9. Send delivery confirmation to customer
        emailService.sendDeliveredConfirmationEmail(shipment);

        log.info("Shipment {} delivered by staff {} at {}",
            shipment.getTrackingId(), staff.getEmail(), shipment.getDeliveredAt());
    }

    // ─────────────────────────────────────────────
    // RESEND OTP
    // ─────────────────────────────────────────────

    @Transactional
    public void resendOtp(String trackingId) {

        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Shipment not found: " + trackingId));

        // Already delivered — nothing to resend
        if (shipment.isOtpVerified()) {
            throw new RuntimeException("Shipment already delivered. Cannot resend OTP.");
        }

        // Shipment not yet at last hub — too early
        if (shipment.getCurrentStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new RuntimeException(
                "Shipment is not out for delivery yet. " +
                "Current status: " + shipment.getCurrentStatus());
        }

        // Generate fresh OTP — resets expiry and attempt counter
        generateAndSendOtp(shipment);
        log.info("OTP resent for shipment {}", trackingId);
    }
}
