package com.deliverytracking.repository;
import com.deliverytracking.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HubRepository extends JpaRepository<Hub, Long> {

    // Only active hubs used for routing
    List<Hub> findByActiveTrue();

    // Bounding box query — only active hubs
    @Query("""
        SELECT h FROM Hub h
        WHERE h.latitude  BETWEEN :minLat AND :maxLat
        AND   h.longitude BETWEEN :minLng AND :maxLng
        AND   h.id NOT IN (:excludeIds)
        AND   h.active = true
        ORDER BY h.latitude DESC
    """)
    List<Hub> findHubsInBoundingBox(
        @Param("minLat")     double minLat,
        @Param("maxLat")     double maxLat,
        @Param("minLng")     double minLng,
        @Param("maxLng")     double maxLng,
        @Param("excludeIds") List<Long> excludeIds
    );
}