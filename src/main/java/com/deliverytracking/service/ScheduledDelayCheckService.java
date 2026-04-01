package com.deliverytracking.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.ShipmentRoute;
import com.deliverytracking.enums.ShipmentStatus;
import com.deliverytracking.repository.ShipmentRepository;
import com.deliverytracking.repository.ShipmentRouteRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//New file: ScheduledDelayCheckService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledDelayCheckService {

 private final ShipmentRepository      shipmentRepository;
 private final ShipmentRouteRepository shipmentRouteRepository;
 private final DeliveryDateService     deliveryDateService;

 // Runs every 6 hours — checks all active shipments for delay
 @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
 @Transactional
 public void checkAllActiveShipments() {
     List<ShipmentStatus> activeStatuses = List.of(
         ShipmentStatus.DISPATCHED,
         ShipmentStatus.IN_TRANSIT,
         ShipmentStatus.DELAYED
     );

     List<Shipment> active = shipmentRepository.findByCurrentStatusIn(activeStatuses);
     log.info("Scheduled delay check — {} active shipments.", active.size());

     for (Shipment shipment : active) {
         try {
             // Find the current step (last ARRIVED or first PENDING)
             int currentStep = shipmentRouteRepository
                 .findByShipmentIdOrderByStepOrder(shipment.getId())
                 .stream()
                 .filter(r -> "ARRIVED".equals(r.getStatus()) || "IN_PROGRESS".equals(r.getStatus()))
                 .mapToInt(ShipmentRoute::getStepOrder)
                 .max()
                 .orElse(0);

             deliveryDateService.checkAndHandleDelay(shipment, currentStep);
         } catch (Exception e) {
             log.error("Delay check failed for shipment {}: {}",
                 shipment.getTrackingId(), e.getMessage());
         }
     }
 }
}
