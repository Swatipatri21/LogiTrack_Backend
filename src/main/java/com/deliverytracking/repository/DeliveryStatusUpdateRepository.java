package com.deliverytracking.repository;

import com.deliverytracking.entity.DeliveryStatusUpdate;
import com.deliverytracking.entity.Shipment;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeliveryStatusUpdateRepository extends JpaRepository<DeliveryStatusUpdate, Long> {

    List<DeliveryStatusUpdate> findAllByShipmentOrderByUpdatedAtDesc(Shipment shipment);

	List<DeliveryStatusUpdate> findByShipmentIdOrderByUpdatedAtDesc(Long id);
	
	@Query("SELECT s FROM DeliveryStatusUpdate s WHERE s.hub.id = :hubId ORDER BY s.updatedAt DESC")
    List<DeliveryStatusUpdate> findByHubId(@Param("hubId") Long hubId);
    
    @Query("""
        SELECT s FROM DeliveryStatusUpdate s 
        WHERE s.hub.id = :hubId 
        AND s.updatedAt BETWEEN :from AND :to 
        ORDER BY s.updatedAt DESC
    """)
    List<DeliveryStatusUpdate> findByHubIdAndDateRange(
        @Param("hubId") Long hubId, 
        @Param("from") LocalDateTime from, 
        @Param("to") LocalDateTime to
    );

	@Modifying
@Transactional
@Query("UPDATE DeliveryStatusUpdate d SET d.updatedBy = null WHERE d.updatedBy.id = :userId")
void nullifyUpdatedBy(@Param("userId") Long userId);
}
