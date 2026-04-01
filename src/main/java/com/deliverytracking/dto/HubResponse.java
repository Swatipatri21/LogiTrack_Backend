package com.deliverytracking.dto;

import lombok.Builder;
import lombok.Data;

//HubResponse.java
@Data
@Builder
public class HubResponse {
 private Long id;
 private String name;
 private String city;
 private double latitude;
 private double longitude;
 private boolean active;
 private Long managerId;
 private String managerName;
 private String managerEmail;
}
