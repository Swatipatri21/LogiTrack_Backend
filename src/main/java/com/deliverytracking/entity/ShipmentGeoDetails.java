package com.deliverytracking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment_geo_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentGeoDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(nullable = false)
    private String origin;              // human-readable pickup address

    @Column(nullable = false)
    private String destination;         // human-readable delivery address

    @Column(nullable = false)
    private double originLat;           // from Nominatim geocoding

    @Column(nullable = false)
    private double originLng;

    @Column(nullable = false)
    private double destinationLat;

    @Column(nullable = false)
    private double destinationLng;
}