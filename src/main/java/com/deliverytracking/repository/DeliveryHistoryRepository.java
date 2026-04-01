package com.deliverytracking.repository;

import com.deliverytracking.entity.DeliveryHistory;
import com.deliverytracking.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeliveryHistoryRepository extends JpaRepository<DeliveryHistory, Long> {

    List<DeliveryHistory> findAllByShipmentOrderByTimestampDesc(Shipment shipment);

    List<DeliveryHistory> findAllByTimestampBetweenOrderByTimestampDesc(LocalDateTime from, LocalDateTime to);
    
    @Query("SELECT h FROM DeliveryHistory h WHERE h.hub.id = :hubId ORDER BY h.timestamp DESC")
    List<DeliveryHistory> findAllByHubId(@Param("hubId") Long hubId);

    @Query("""
        SELECT h FROM DeliveryHistory h 
        WHERE h.hub.id = :hubId 
        AND h.timestamp BETWEEN :from AND :to 
        ORDER BY h.timestamp DESC
    """)
    List<DeliveryHistory> findAllByHubIdAndDateRange(
        @Param("hubId") Long hubId, 
        @Param("from") LocalDateTime from, 
        @Param("to") LocalDateTime to
    );
}
