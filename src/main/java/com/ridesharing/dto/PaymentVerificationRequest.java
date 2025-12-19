package com.ridesharing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for payment verification
 */
@Data
public class PaymentVerificationRequest {
    
    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;
    
    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;
    
    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
    
    // Optional fields from Razorpay response
    private String paymentMethod;
    private String gatewayResponse;
}