package com.deliverytracking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String performedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id") 
    private Hub hub;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;
}
