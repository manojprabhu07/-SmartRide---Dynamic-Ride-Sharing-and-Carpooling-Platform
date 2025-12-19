package com.ridesharing.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Entity - Stores all payment transaction details
 * Linked to Booking for complete payment tracking
 */
@Entity
@Table(name = "payments")
@Data
@EqualsAndHashCode(exclude = {"booking"})
@ToString(exclude = {"booking"})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationship with Booking (One Payment per Booking)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Razorpay Order Details
    @Column(name = "razorpay_order_id", unique = true, nullable = false)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    // Payment Amount Details
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    // Payment Status
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.CREATED;

    // Payment Method (UPI, Card, NetBanking, etc.)
    @Column(name = "payment_method")
    private String paymentMethod;

    // Razorpay Response Details
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    // Additional Payment Info
    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "description")
    private String description;

    // Driver Settlement Details
    @Column(name = "driver_settlement_amount", precision = 10, scale = 2)
    private BigDecimal driverSettlementAmount;

    @Column(name = "platform_commission", precision = 10, scale = 2)
    private BigDecimal platformCommission;

    @Column(name = "settlement_status")
    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Auto-generate timestamps
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Generate receipt number
        if (this.receiptNumber == null) {
            this.receiptNumber = "SR" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        
        // Set paid timestamp when payment is completed
        if (this.paymentStatus == PaymentStatus.COMPLETED && this.paidAt == null) {
            this.paidAt = LocalDateTime.now();
        }
    }

    /**
     * Calculate driver settlement amount (after platform commission)
     */
    public void calculateSettlement(BigDecimal commissionPercentage) {
        if (this.amount != null) {
            this.platformCommission = this.amount.multiply(commissionPercentage).divide(BigDecimal.valueOf(100));
            this.driverSettlementAmount = this.amount.subtract(this.platformCommission);
        }
    }

    /**
     * Payment Status Enum
     */
    public enum PaymentStatus {
        CREATED,        // Order created in Razorpay
        PENDING,        // Payment initiated by user
        COMPLETED,      // Payment successful
        FAILED,         // Payment failed
        REFUNDED,       // Payment refunded
        CANCELLED       // Payment cancelled
    }

    /**
     * Settlement Status Enum
     */
    public enum SettlementStatus {
        PENDING,        // Waiting for ride completion
        COMPLETED,      // Amount settled to driver
        FAILED,         // Settlement failed
        CANCELLED       // Settlement cancelled (refund case)
    }
}