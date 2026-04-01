package com.deliverytracking.entity;

import com.deliverytracking.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trackingId;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String senderAddress;
    private String senderPhone;
    private String senderEmail;

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverAddress;

    @Column(nullable = false)
    private String receiverPhone;

    @Column(nullable = false)
    private String receiverEmail;       // ADD — needed to send OTP email

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus currentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Geo routing fields ──────────────────────
    private double originLat;
    private double originLng;
    private double destinationLat;
    private double destinationLng;

    // ── OTP delivery confirmation fields ────────
    @Column(name = "otp_hash")
    private String otpHash;             // BCrypt hash of OTP, null after delivery

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;    // 15 minutes from generation

    @Column(name = "otp_verified")
    private boolean otpVerified;        // true after successful delivery confirmation

    @Column(name = "otp_attempts")
    private int otpAttempts;            // increments on wrong OTP, max 3

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;  // timestamp of confirmed delivery

    @Column(name = "delivered_by_staff_id")
    private Long deliveredByStaffId;    // staff who confirmed delivery
    
 // Add these fields to Shipment.java
    
    @Column(name = "expected_delivery_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Kolkata")
    private LocalDateTime expectedDeliveryDate;     // calculated on creation

    @Column(name = "revised_delivery_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Kolkata")
    private LocalDateTime revisedDeliveryDate;      // updated if delay detected
    
    @Builder.Default
    @Column(name = "is_delayed")
    private boolean isDelayed = false;              // true if behind schedule

    @Column(name = "delay_reason")
    private String delayReason; 
    
 // In Shipment.java — add these
    @Builder.Default
    @Column(name = "delivery_attempts")
    private  Integer deliveryAttempts = 0;      // increments on each failed attempt
     
    @Builder.Default
    @Column(name = "max_delivery_attempts")
    private Integer maxDeliveryAttempts = 3;   // after this → return to sender

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;   // when last delivery was tried

    @Column(name = "attempt_failure_reason")
    private String attemptFailureReason;   // CUSTOMER_ABSENT, REFUSED, WRONG_ADDRESS
}
