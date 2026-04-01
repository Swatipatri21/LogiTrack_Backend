package com.deliverytracking.repository;

import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.User;
import com.deliverytracking.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingId(String trackingId);

    List<Shipment> findAllByCreatedBy(User createdBy);

    List<Shipment> findAllByCurrentStatus(ShipmentStatus status);

    boolean existsByTrackingId(String trackingId);

	List<Shipment> findByCurrentStatusIn(List<ShipmentStatus> activeStatuses);
}
