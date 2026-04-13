package com.deliverytracking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_delivery_dates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDeliveryDates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @Column(name = "revised_delivery_date")
    private LocalDateTime revisedDeliveryDate;

    @Builder.Default
    @Column(name = "is_delayed")
    private boolean isDelayed = false;

    @Column(name = "delay_reason")
    private String delayReason;
}