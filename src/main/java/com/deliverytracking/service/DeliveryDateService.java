package com.deliverytracking.service;

import com.deliverytracking.entity.DeliveryStatusUpdate;
import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.ShipmentRoute;
import com.deliverytracking.enums.ShipmentStatus;
import com.deliverytracking.repository.DeliveryStatusUpdateRepository;
import com.deliverytracking.repository.ShipmentRepository;
import com.deliverytracking.repository.ShipmentRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryDateService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentRouteRepository shipmentRouteRepository;
    private final DeliveryStatusUpdateRepository statusUpdateRepository;
    
//    private static final int HOURS_PER_HUB      = 12;
    private static final int DELAY_THRESHOLD_HOURS = 6;  // hours over expected = delay
    private static final int EARLY_THRESHOLD_HOURS = 2;  // hours under expected = ahead

    private static final int    DELIVERY_START_HOUR = 9;
    private static final int    DELIVERY_END_HOUR   = 19;
    private static final double AVG_SPEED_KMPH      = 60.0;  // avg truck/courier speed
    private static final double HOURS_PER_HUB       = 12.0;  // processing time per hub stop
    private static final double MIN_HOURS            = 6.0;   // same-city minimum
    private static final double LOCAL_THRESHOLD_KM  = 50.0;  // within city/local
    private static final double SHORT_THRESHOLD_KM  = 300.0; // short haul
    private static final double MEDIUM_THRESHOLD_KM = 800.0; // medium haul

  
    public LocalDateTime calculateExpectedDeliveryDate(
            double originLat, double originLng,
            double destLat,   double destLng,
            int totalHubs) {

        double distanceKm   = haversineDistance(originLat, originLng, destLat, destLng);
        double transitHours = calculateTransitHours(distanceKm);
        double hubOverhead  = (totalHubs > 1) ? (totalHubs - 1) * HOURS_PER_HUB : 0;
        double totalHours   = transitHours + hubOverhead;

        // Buffer: 20% for traffic, loading, delays — rounded up
        totalHours = Math.ceil(totalHours * 1.2);

        // Enforce minimum
        totalHours = Math.max(totalHours, MIN_HOURS);

        return snapToDeliveryWindow(LocalDateTime.now().plusHours((long) totalHours));
    }

   
    private double calculateTransitHours(double distanceKm) {
        if (distanceKm <= LOCAL_THRESHOLD_KM) {
            // Same city / very close — 6h flat
            return 6.0;
        } else if (distanceKm <= SHORT_THRESHOLD_KM) {
            // < 300km — drive time + 12h handling buffer
            return (distanceKm / AVG_SPEED_KMPH) + 12.0;
        } else if (distanceKm <= MEDIUM_THRESHOLD_KM) {
            // 300–800km — drive time + 24h handling
            return (distanceKm / AVG_SPEED_KMPH) + 24.0;
        } else {
            // > 800km long haul — drive time + 48h for loading/unloading/multi-day
            return (distanceKm / AVG_SPEED_KMPH) + 48.0;
        }
    }

    
    private LocalDateTime snapToDeliveryWindow(LocalDateTime rawDate) {
        int hour = rawDate.getHour();
        if (hour < DELIVERY_START_HOUR) {
            return rawDate.withHour(DELIVERY_START_HOUR).withMinute(0).withSecond(0);
        } else if (hour >= DELIVERY_END_HOUR) {
            return rawDate.plusDays(1)
                          .withHour(DELIVERY_START_HOUR)
                          .withMinute(0)
                          .withSecond(0);
        }
        return rawDate.withSecond(0);
    }

    
    public double haversineDistance(double lat1, double lng1,
                                    double lat2, double lng2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
     @Deprecated
    public LocalDateTime calculateExpectedDeliveryDate(int totalHubs) {
        int totalHours;
        if (totalHubs == 1)      totalHours = 6;
        else if (totalHubs == 2) totalHours = 72;
        else                     totalHours = (int) ((totalHubs * HOURS_PER_HUB) + 72);

        LocalDateTime rawDate = LocalDateTime.now().plusHours(totalHours);
        if (rawDate.getHour() < 9)
            rawDate = rawDate.withHour(9).withMinute(0);
        else if (rawDate.getHour() >= 19)
            rawDate = rawDate.plusDays(1).withHour(9).withMinute(0);
        return rawDate;
    }

 
//     @Transactional
//     public void checkAndHandleDelay(Shipment shipment, int currentStepOrder) {
//         List<ShipmentRoute> allSteps = shipmentRouteRepository
//             .findByShipmentIdOrderByStepOrder(shipment.getId());
//
//         int totalSteps     = allSteps.size();
//         int remainingSteps = totalSteps - currentStepOrder - 1;
//
//         LocalDateTime now          = LocalDateTime.now();
//         LocalDateTime expectedDate = shipment.getExpectedDeliveryDate();
//
//         if (expectedDate == null) {
//             log.warn("Shipment {} has no expectedDeliveryDate — skipping delay check.",
//                 shipment.getTrackingId());
//             return;
//         }
//
//         // How many hours until/past the expected delivery date
//         long hoursUntilExpected = java.time.Duration.between(now, expectedDate).toHours();
//         // Negative = past due, Positive = still on time
//
//         // Estimate: how many hours per step should have elapsed
//         // Based on total expected time divided equally across steps
//         long totalExpectedHours = java.time.Duration.between(
//             shipment.getCreatedAt(), expectedDate).toHours();
//         long expectedHoursAtStep = totalSteps > 0
//             ? (totalExpectedHours * currentStepOrder) / totalSteps
//             : 0;
//
//         long hoursElapsed = java.time.Duration.between(shipment.getCreatedAt(), now).toHours();
//         long diff         = hoursElapsed - expectedHoursAtStep; // positive = late
//
//         if (diff > DELAY_THRESHOLD_HOURS) {
//             // ── LATE ──────────────────────────────────────────────────────────
//             long remainingHoursEstimate = remainingSteps > 0
//                 ? (totalExpectedHours / totalSteps) * remainingSteps
//                 : 6L;
//
//             LocalDateTime revisedDate = snapToDeliveryWindow(
//                 now.plusHours((long)(remainingHoursEstimate * 1.2))
//             );
//
//             if (!shipment.isDelayed()) {
//                 shipment.setDelayed(true);
//                 shipment.setDelayReason("Running " + diff + " hours behind schedule.");
//                 shipment.setRevisedDeliveryDate(revisedDate);
//                 shipmentRepository.save(shipment);
//
//                 saveSystemUpdate(shipment, ShipmentStatus.DELAYED,
//                     "Shipment is running " + diff + " hours behind schedule. "
//                     + "Revised delivery: " + revisedDate + ".");
//
//                 log.warn("Auto-delay flagged for {} — {} hours overdue.",
//                     shipment.getTrackingId(), diff);
//             } else {
//                 // Already delayed — just update revised date
//                 shipment.setRevisedDeliveryDate(revisedDate);
//                 shipmentRepository.save(shipment);
//                 log.info("Revised date updated for already-delayed shipment {}.",
//                     shipment.getTrackingId());
//             }
//
//         } else if (diff < -EARLY_THRESHOLD_HOURS) {
//             // ── AHEAD OF SCHEDULE ─────────────────────────────────────────────
//             if (shipment.isDelayed()) {
//                 long newHours = remainingSteps > 0
//                     ? (totalExpectedHours / totalSteps) * remainingSteps
//                     : 6L;
//                 LocalDateTime revisedDate = snapToDeliveryWindow(now.plusHours(newHours));
//
//                 shipment.setDelayed(false);
//                 shipment.setDelayReason(null);
//                 shipment.setRevisedDeliveryDate(revisedDate);
//                 shipmentRepository.save(shipment);
//
//                 saveSystemUpdate(shipment, ShipmentStatus.IN_TRANSIT,
//                     "Shipment recovered — now ahead of schedule. Revised delivery: " + revisedDate);
//
//                 log.info("Shipment {} recovered from delay.", shipment.getTrackingId());
//             }
//
//         } else {
//             // ── ON TIME ───────────────────────────────────────────────────────
//             if (shipment.isDelayed()) {
//                 shipment.setDelayed(false);
//                 shipment.setDelayReason(null);
//                 shipmentRepository.save(shipment);
//                 saveSystemUpdate(shipment, ShipmentStatus.IN_TRANSIT,
//                     "Shipment is back on track.");
//                 log.info("Shipment {} back on track.", shipment.getTrackingId());
//             }
//         }
//     }
     
     
     @Transactional
     public void checkAndHandleDelay(Shipment shipment, int currentStepOrder) {

         List<ShipmentRoute> allSteps = shipmentRouteRepository
             .findByShipmentIdOrderByStepOrder(shipment.getId());

         if (allSteps.isEmpty()) return;

         int totalSteps     = allSteps.size();
         int remainingSteps = totalSteps - currentStepOrder - 1;

         LocalDateTime now          = LocalDateTime.now();
         LocalDateTime expectedDate = shipment.getExpectedDeliveryDate();

         // Total window from creation to expected delivery
         if (shipment.getCreatedAt() == null || expectedDate == null) {
    log.warn("Skipping delay check for {} — missing createdAt or expectedDate", 
        shipment.getTrackingId());
    return;
}
         long totalExpectedHours = java.time.Duration.between(
             shipment.getCreatedAt(), expectedDate).toHours();

         if (totalExpectedHours <= 0) return;

         // How many hours should have elapsed by this step (proportional)
         long expectedHoursAtStep = (totalExpectedHours * currentStepOrder) / totalSteps;
         long hoursElapsed        = java.time.Duration.between(shipment.getCreatedAt(), now).toHours();
         long diff                = hoursElapsed - expectedHoursAtStep; // + = late, - = early

         log.info("Delay check for {}: step={}/{}, elapsed={}h, expectedAtStep={}h, diff={}h",
             shipment.getTrackingId(), currentStepOrder, totalSteps,
             hoursElapsed, expectedHoursAtStep, diff);

         if (diff > DELAY_THRESHOLD_HOURS) {

             // Recalculate revised date: remaining steps proportionally
             long hoursPerStep    = totalExpectedHours / totalSteps;
             long remainingHours  = (long) Math.ceil(remainingSteps * hoursPerStep * 1.2);
             LocalDateTime revised = snapToDeliveryWindow(now.plusHours(Math.max(remainingHours, 6)));

             if (!shipment.isDelayed()) {
                 shipment.setDelayed(true);
                 shipment.setDelayReason("Running " + diff + " hours behind schedule.");
                 shipment.setRevisedDeliveryDate(revised);
                 shipmentRepository.save(shipment);

                 saveSystemUpdate(shipment, ShipmentStatus.DELAYED,
                     "Shipment is running " + diff + " hours behind schedule. "
                     + "Revised delivery: " + revised + ".");

                 log.warn("Auto-delay flagged for {} — {} hours overdue.",
                     shipment.getTrackingId(), diff);
             } else {
                 // Already delayed — just update revised date
                 shipment.setRevisedDeliveryDate(revised);
                 shipmentRepository.save(shipment);
                 log.info("Revised date updated for already-delayed shipment {}.",
                     shipment.getTrackingId());
             }

         } else if (diff < -EARLY_THRESHOLD_HOURS && shipment.isDelayed()) {

             // Was delayed, now ahead — clear delay
             long hoursPerStep   = totalExpectedHours / totalSteps;
             long remainingHours = remainingSteps * hoursPerStep;
             LocalDateTime revised = snapToDeliveryWindow(now.plusHours(Math.max(remainingHours, 6)));

             shipment.setDelayed(false);
             shipment.setDelayReason(null);
             shipment.setRevisedDeliveryDate(revised);
             shipmentRepository.save(shipment);

             saveSystemUpdate(shipment, ShipmentStatus.IN_TRANSIT,
                 "Shipment recovered and is ahead of schedule. Revised delivery: " + revised + ".");

             log.info("Shipment {} recovered from delay.", shipment.getTrackingId());

         } else if (Math.abs(diff) <= DELAY_THRESHOLD_HOURS && shipment.isDelayed()) {

             // Back on time — clear delay flag
             shipment.setDelayed(false);
             shipment.setDelayReason(null);
             shipmentRepository.save(shipment);

             saveSystemUpdate(shipment, ShipmentStatus.IN_TRANSIT,
                 "Shipment is back on track.");

             log.info("Shipment {} back on track.", shipment.getTrackingId());
         }
     }

    // ─────────────────────────────────────────────
    // MANUAL DELAY REPORT — admin / hub manager / staff
    // ─────────────────────────────────────────────

    @Transactional
    public void reportDelay(String trackingId, String reason, int additionalHours) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
            .orElseThrow(() -> new RuntimeException("Shipment not found: " + trackingId));

        if (shipment.getCurrentStatus() == ShipmentStatus.DELIVERED)
            throw new RuntimeException("Cannot report delay on a delivered shipment.");

        if (shipment.getCurrentStatus() == ShipmentStatus.OUT_FOR_DELIVERY)
            throw new RuntimeException(
                "Shipment is already out for delivery. Cannot update delivery date.");

        LocalDateTime currentExpected = shipment.getRevisedDeliveryDate() != null
            ? shipment.getRevisedDeliveryDate()
            : shipment.getExpectedDeliveryDate();
        if (currentExpected == null) {
   currentExpected = calculateExpectedDeliveryDate(
    shipment.getOriginLat(),    shipment.getOriginLng(),
    shipment.getDestinationLat(), shipment.getDestinationLng(),
    shipmentRouteRepository.countByShipmentId(shipment.getId())
);
    shipment.setExpectedDeliveryDate(currentExpected);
    shipmentRepository.save(shipment);
    log.info("Backfilled expectedDeliveryDate for {} during delay report", trackingId);
}
        LocalDateTime newRevisedDate = currentExpected.plusHours(additionalHours);

        shipment.setDelayed(true);
        shipment.setRevisedDeliveryDate(newRevisedDate);
        shipment.setDelayReason(reason);
        shipmentRepository.save(shipment);

        saveSystemUpdate(shipment, ShipmentStatus.DELAYED,
            "⏳ Delivery delayed by " + additionalHours + " hours. "
            + "Reason: " + reason + ". "
            + "Revised delivery date: " + newRevisedDate + ".");

        log.info("Manual delay reported for {} — reason: {}, +{}h, new date: {}",
            trackingId, reason, additionalHours, newRevisedDate);
    }

    // ─────────────────────────────────────────────
    // HELPER — unified system timeline entry
    // ─────────────────────────────────────────────

    private void saveSystemUpdate(Shipment shipment, ShipmentStatus status, String remarks) {
        DeliveryStatusUpdate update = new DeliveryStatusUpdate();
        update.setShipment(shipment);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        update.setLocation("System");
        update.setRemarks(remarks);
        // updatedBy is null for system-generated entries — handle this in mapToResponse
        statusUpdateRepository.save(update);
    }
}
