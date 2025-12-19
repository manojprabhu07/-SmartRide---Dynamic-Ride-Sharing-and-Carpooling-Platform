package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {
    
    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private BigDecimal driverSettlementAmount;
    private BigDecimal platformCommission;
    private String paymentStatus;
    private String settlementStatus;
    private String paymentMethod;
    private String receiptNumber;
    private String description;
    
    // Booking details
    private String source;
    private String destination;
    private String passengerName;
    private String driverName;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime settlementDate;
}