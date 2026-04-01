package com.deliverytracking.entity;

import com.deliverytracking.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false,length = 50)
    private LocalDateTime createdAt;
    
//    @ManyToOne
//    @JoinColumn(name = "hub_id")
//    private Hub hub;  
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    @JsonIgnoreProperties({"staff", "hibernateLazyInitializer", "handler"})  // ADD
    private Hub hub;
}
