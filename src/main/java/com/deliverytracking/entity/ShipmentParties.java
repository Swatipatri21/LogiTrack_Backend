package com.deliverytracking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment_parties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentParties {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String senderAddress;

    @Column(nullable = false)
    private String senderPhone;

    @Column(nullable = false)
    private String senderEmail;

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverAddress;

    @Column(nullable = false)
    private String receiverPhone;

    @Column(nullable = false)
    private String receiverEmail;
}
