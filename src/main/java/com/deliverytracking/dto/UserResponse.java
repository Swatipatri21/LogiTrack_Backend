package com.deliverytracking.dto;

import com.deliverytracking.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Long hubId;
    private String hubName;
    private Role role;
    private LocalDateTime createdAt;
}
