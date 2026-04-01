package com.deliverytracking.entity;

import com.deliverytracking.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OnDelete;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_status_updates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatusUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
     
    
    @ManyToOne
    @JoinColumn(name = "updated_by", nullable = true, 
        foreignKey = @ForeignKey(name = "FKet8977jd1w2k3opxtngppkebr"))
    @OnDelete(action = OnDeleteAction.SET_NULL)  // ← add this
    private User updatedBy;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "updated_by")
//    private User updatedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private String location;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime updatedAt;
}
