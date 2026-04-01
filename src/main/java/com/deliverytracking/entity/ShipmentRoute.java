package com.deliverytracking.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "shipment_routes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentRoute {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Shipment shipment;

    @ManyToOne
    private Hub hub;

    private int stepOrder;          // 0 = origin hub, 1 = first intermediate, etc.
    private boolean isUnlocked;     // only step 0 starts unlocked
    private String status;          // PENDING, IN_TRANSIT, ARRIVED, DELIVERED
    private LocalDateTime updatedAt;
    private Long updatedByUserId;
}