package com.ridesharing.controller;

import com.ridesharing.dto.PaymentOrderRequest;
import com.ridesharing.dto.PaymentOrderResponse;
import com.ridesharing.dto.PaymentVerificationRequest;
import com.ridesharing.dto.PaymentHistoryResponse;
import com.ridesharing.exception.PaymentException;
import com.ridesharing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Payment Controller - REST endpoints for payment operations
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
    originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://localhost:*"}, 
    allowCredentials = "true"
)
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create payment order for booking
     * POST /api/payments/create-order
     */
    @PostMapping("/create-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createPaymentOrder(@Valid @RequestBody PaymentOrderRequest request) {
        try {
            log.info("💳 Payment order request received for booking: {}", request.getBookingId());
            
            PaymentOrderResponse response = paymentService.createPaymentOrder(request);
            
            log.info("✅ Payment order created successfully: {}", response.getOrderId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment order created successfully",
                    "data", response
            ));
        } catch (PaymentException e) {
            log.error("Payment order creation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error during payment order creation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to create payment order"
            ));
        }
    }

    /**
     * Verify payment signature and complete payment
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        try {
            boolean verified = paymentService.verifyPayment(request);
            
            if (verified) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Payment verified successfully",
                        "verified", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment verification failed",
                        "verified", false
                ));
            }
        } catch (PaymentException e) {
            log.error("Payment verification failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "verified", false
            ));
        } catch (Exception e) {
            log.error("Unexpected error during payment verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Payment verification failed",
                    "verified", false
            ));
        }
    }

    /**
     * Handle payment failure
     * POST /api/payments/failure
     */
    @PostMapping("/failure")
    public ResponseEntity<?> handlePaymentFailure(@RequestBody Map<String, String> failureData) {
        try {
            String orderId = failureData.get("razorpay_order_id");
            String reason = failureData.get("reason");
            
            paymentService.handlePaymentFailure(orderId, reason);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment failure handled successfully"
            ));
        } catch (Exception e) {
            log.error("Error handling payment failure", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to handle payment failure"
            ));
        }
    }

    /**
     * Get payment history for passenger
     * GET /api/payments/history/passenger/{passengerId}
     */
    @GetMapping("/history/passenger/{passengerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPassengerPaymentHistory(@PathVariable Long passengerId) {
        try {
            List<PaymentHistoryResponse> history = paymentService.getPassengerPaymentHistory(passengerId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment history retrieved successfully",
                    "data", history
            ));
        } catch (Exception e) {
            log.error("Error retrieving passenger payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to retrieve payment history"
            ));
        }
    }

    /**
     * Get payment history for driver (earnings)
     * GET /api/payments/history/driver/{driverId}
     */
    @GetMapping("/history/driver/{driverId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDriverPaymentHistory(@PathVariable Long driverId) {
        try {
            List<PaymentHistoryResponse> history = paymentService.getDriverPaymentHistory(driverId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Driver earnings history retrieved successfully",
                    "data", history
            ));
        } catch (Exception e) {
            log.error("Error retrieving driver payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to retrieve earnings history"
            ));
        }
    }

    /**
     * Get payment history within date range
     * GET /api/payments/history/{userId}?isDriver=true&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GetMapping("/history/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPaymentHistoryByDateRange(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean isDriver,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<PaymentHistoryResponse> history = paymentService.getPaymentHistoryByDateRange(
                    userId, isDriver, startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment history retrieved successfully",
                    "data", history
            ));
        } catch (Exception e) {
            log.error("Error retrieving payment history by date range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to retrieve payment history"
            ));
        }
    }

    /**
     * Release payment to driver after ride completion
     * POST /api/payments/release/{bookingId}
     */
    @PostMapping("/release/{bookingId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    public ResponseEntity<?> releasePaymentToDriver(@PathVariable Long bookingId) {
        try {
            paymentService.releasePaymentToDriver(bookingId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment released to driver successfully"
            ));
        } catch (PaymentException e) {
            log.error("Payment release failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error during payment release", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to release payment"
            ));
        }
    }

    /**
     * Get total earnings for driver
     * GET /api/payments/earnings/{driverId}
     */
    @GetMapping("/earnings/{driverId}")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<?> getDriverTotalEarnings(@PathVariable Long driverId) {
        try {
            BigDecimal totalEarnings = paymentService.getDriverTotalEarnings(driverId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Total earnings retrieved successfully",
                    "totalEarnings", totalEarnings
            ));
        } catch (Exception e) {
            log.error("Error retrieving driver total earnings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to retrieve total earnings"
            ));
        }
    }

    /**
     * Get total spending for passenger
     * GET /api/payments/spending/{passengerId}
     */
    @GetMapping("/spending/{passengerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPassengerTotalSpending(@PathVariable Long passengerId) {
        try {
            BigDecimal totalSpending = paymentService.getPassengerTotalSpending(passengerId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Total spending retrieved successfully",
                    "totalSpending", totalSpending
            ));
        } catch (Exception e) {
            log.error("Error retrieving passenger total spending", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to retrieve total spending"
            ));
        }
    }

    /**
     * Test endpoint to check payment configuration
     * GET /api/payments/test
     */
    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testPaymentConfiguration() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment service is configured and running",
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Check instant payment status for a specific ride (for drivers)
     * GET /api/payments/instant-status/{rideId}
     */
    @GetMapping("/instant-status/{rideId}")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<?> checkInstantPaymentStatus(@PathVariable Long rideId) {
        try {
            // This would check all bookings for the ride and their payment status
            // For now, return a simple status
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Instant payment system is active",
                    "rideId", rideId,
                    "autoSettlementEnabled", true,
                    "description", "Payments will be automatically settled when you mark ride as complete"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to check payment status"
            ));
        }
    }

    /**
     * Get driver earnings summary with real-time status
     * GET /api/payments/earnings/{driverId}/summary
     */
    @GetMapping("/earnings/{driverId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDriverEarningsSummary(@PathVariable Long driverId) {
        try {
            List<PaymentHistoryResponse> allPayments = paymentService.getDriverPaymentHistory(driverId);
            BigDecimal totalEarnings = paymentService.getDriverTotalEarnings(driverId);
            
            // Calculate summary statistics
            BigDecimal pendingEarnings = allPayments.stream()
                    .filter(p -> "PENDING".equals(p.getSettlementStatus()))
                    .map(p -> p.getDriverSettlementAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
            BigDecimal completedEarnings = allPayments.stream()
                    .filter(p -> "COMPLETED".equals(p.getSettlementStatus()))
                    .map(p -> p.getDriverSettlementAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
            // Today's earnings
            BigDecimal todayEarnings = allPayments.stream()
                    .filter(p -> "COMPLETED".equals(p.getSettlementStatus()) && 
                                p.getSettlementDate() != null &&
                                p.getSettlementDate().toLocalDate().equals(java.time.LocalDate.now()))
                    .map(p -> p.getDriverSettlementAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Driver earnings summary retrieved successfully",
                    "data", Map.of(
                            "totalEarnings", totalEarnings,
                            "pendingEarnings", pendingEarnings,
                            "completedEarnings", completedEarnings,
                            "todayEarnings", todayEarnings,
                            "totalTransactions", allPayments.size(),
                            "completedTransactions", allPayments.stream()
                                    .mapToInt(p -> "COMPLETED".equals(p.getPaymentStatus()) ? 1 : 0)
                                    .sum()
                    )
            ));
        } catch (Exception e) {
            log.error("Error retrieving driver earnings summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to retrieve earnings summary"
            ));
        }
    }

    /**
     * Debug endpoint to test API connectivity
     * GET /api/payments/debug/test
     */
    @GetMapping("/debug/test")
    public ResponseEntity<?> debugTest() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment API is working",
                "timestamp", System.currentTimeMillis(),
                "mock_mode", true
        ));
    }
}