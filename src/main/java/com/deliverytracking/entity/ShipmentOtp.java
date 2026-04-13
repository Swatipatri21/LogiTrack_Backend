package com.deliverytracking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(name = "otp_hash")
    private String otpHash;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "otp_verified")
    private boolean otpVerified;

    @Column(name = "otp_attempts")
    private int otpAttempts;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivered_by_staff_id")
    private Long deliveredByStaffId;

    @Builder.Default
    @Column(name = "delivery_attempts")
    private Integer deliveryAttempts = 0;

    @Builder.Default
    @Column(name = "max_delivery_attempts")
    private Integer maxDeliveryAttempts = 3;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "attempt_failure_reason")
    private String attemptFailureReason;
}