package com.ridesharing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating payment order
 */
@Data
public class PaymentOrderRequest {
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
    
    // Optional fields for additional payment info
    private String notes;
    private String customerEmail;
    private String customerPhone;
}