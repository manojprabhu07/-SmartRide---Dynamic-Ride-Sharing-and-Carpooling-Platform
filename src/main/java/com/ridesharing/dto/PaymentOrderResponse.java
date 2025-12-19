package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for payment order creation
 * Contains all data needed for Razorpay checkout
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    
    private String orderId;           // Razorpay order ID
    private BigDecimal amount;        // Amount in rupees
    private String currency;          // Currency (INR)
    private String keyId;            // Razorpay key ID for frontend
    private String companyName;      // Company name for checkout
    private String description;      // Payment description
    private String contactEmail;     // Customer email
    private String contactPhone;     // Customer phone
    private Long bookingId;          // Associated booking ID
    private Long paymentId;          // Internal payment record ID
}