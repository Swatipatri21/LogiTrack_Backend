package com.deliverytracking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequest {

    @NotBlank(message = "Sender name is required")
    private String senderName;

    @NotBlank(message = "Sender address is required")
    private String senderAddress;
    
    @NotBlank(message = "Sender phone is required")
    private String senderPhone;

    @NotBlank(message = "Sender email is required")
    @Email(message = "Invalid sender email format")
    private String senderEmail;

    @NotBlank(message = "Receiver name is required")
    private String receiverName;

    @NotBlank(message = "Receiver address is required")
    private String receiverAddress;

    @NotBlank(message = "Receiver phone is required")
    private String receiverPhone;

    @NotBlank(message = "Receiver email is required")   // ADD
    @Email(message = "Invalid email format")             // ADD
    private String receiverEmail;                        // ADD

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    @Max(value = 1000, message = "Weight cannot exceed 1000 kg")
    private Double weight;

    private String description;
}