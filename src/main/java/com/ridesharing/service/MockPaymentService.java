package com.ridesharing.service;

import com.ridesharing.dto.PaymentOrderRequest;
import com.ridesharing.dto.PaymentOrderResponse;
import com.ridesharing.dto.PaymentVerificationRequest;
import com.ridesharing.dto.PaymentHistoryResponse;
import com.ridesharing.entity.Booking;
import com.ridesharing.entity.Payment;
import com.ridesharing.entity.Ride;
import com.ridesharing.exception.PaymentException;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mock Payment Service for testing without actual Razorpay credentials
 * Enable with spring.profiles.active=mock-payment
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("mock-payment")
public class MockPaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${app.platform.commission:10.0}")
    private BigDecimal platformCommissionPercentage;

    @Transactional
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request) {
        log.info("🧪 MOCK: Creating payment order for booking ID: {}", request.getBookingId());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new PaymentException("Booking not found"));

        // Create mock payment record
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency(currency);
        payment.setPaymentStatus(Payment.PaymentStatus.CREATED);
        payment.setSettlementStatus(Payment.SettlementStatus.PENDING);
        payment.calculateSettlement(platformCommissionPercentage);
        payment.setCreatedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        // Generate mock Razorpay order ID
        String mockOrderId = "order_mock_" + UUID.randomUUID().toString().substring(0, 10);

        log.info("✅ MOCK: Payment order created - Order ID: {}, Amount: ₹{}", 
                mockOrderId, booking.getTotalAmount());

        return PaymentOrderResponse.builder()
                .orderId(mockOrderId)
                .amount(booking.getTotalAmount())
                .currency(currency)
                .keyId("rzp_test_MOCK_KEY_ID") // Mock Razorpay key to avoid 500 errors
                .companyName("SmartRide (Test Mode)")
                .description("Mock payment for ride from " + booking.getRide().getSource() + " to " + booking.getRide().getDestination())
                .contactEmail(booking.getPassenger().getEmail())
                .contactPhone(booking.getPassenger().getPhoneNumber())
                .bookingId(booking.getId())
                .paymentId(payment.getId())
                .build();
    }

    @Transactional
    public String verifyPayment(PaymentVerificationRequest request) {
        log.info("🧪 MOCK: Verifying payment - Order: {}, Payment: {}", 
                request.getRazorpayOrderId(), request.getRazorpayPaymentId());

        // Mock payment verification (always success in test mode)
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentException("Payment not found"));

        // Update payment with mock Razorpay details
        payment.setRazorpayOrderId(request.getRazorpayOrderId());
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature("mock_signature_" + UUID.randomUUID().toString().substring(0, 10));
        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        paymentRepository.save(payment);

        log.info("✅ MOCK: Payment verified successfully - Payment ID: {}", payment.getId());
        return "Payment verified successfully";
    }

    public List<PaymentHistoryResponse> getPaymentHistory(Long userId, String userType) {
        log.info("🧪 MOCK: Fetching payment history for user: {} ({})", userId, userType);
        if ("PASSENGER".equals(userType)) {
            return getPassengerPaymentHistory(userId);
        } else {
            return getDriverPaymentHistory(userId);
        }
    }

    public List<PaymentHistoryResponse> getPassengerPaymentHistory(Long passengerId) {
        log.info("🧪 MOCK: Fetching passenger payment history for passenger: {}", passengerId);
        return paymentRepository.findByPassengerId(passengerId)
                .stream()
                .map(this::mapToPaymentHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentHistoryResponse> getDriverPaymentHistory(Long driverId) {
        log.info("🧪 MOCK: Fetching driver payment history for driver: {}", driverId);
        return paymentRepository.findByDriverId(driverId)
                .stream()
                .map(this::mapToPaymentHistoryResponse)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalEarnings(Long driverId) {
        log.info("🧪 MOCK: Fetching total earnings for driver: {}", driverId);
        Double earnings = paymentRepository.getTotalEarningsByDriverId(driverId);
        return earnings != null ? BigDecimal.valueOf(earnings) : BigDecimal.ZERO;
    }

    public BigDecimal getTotalSpending(Long passengerId) {
        log.info("🧪 MOCK: Fetching total spending for passenger: {}", passengerId);
        Double spending = paymentRepository.getTotalSpendingByPassengerId(passengerId);
        return spending != null ? BigDecimal.valueOf(spending) : BigDecimal.ZERO;
    }

    /**
     * Map Payment entity to PaymentHistoryResponse DTO
     */
    private PaymentHistoryResponse mapToPaymentHistoryResponse(Payment payment) {
        Booking booking = payment.getBooking();
        Ride ride = booking.getRide();
        return PaymentHistoryResponse.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .amount(payment.getAmount())
                .driverSettlementAmount(payment.getDriverSettlementAmount())
                .platformCommission(payment.getPlatformCommission())
                .paymentStatus(payment.getPaymentStatus().toString())
                .settlementStatus(payment.getSettlementStatus().toString())
                .paymentMethod("MOCK_RAZORPAY")
                .receiptNumber("MOCK_" + payment.getId())
                .description("Mock payment for ride from " + ride.getSource() + " to " + ride.getDestination())
                .source(ride.getSource())
                .destination(ride.getDestination())
                .passengerName(booking.getPassenger().getFirstName() + " " + booking.getPassenger().getLastName())
                .driverName(ride.getDriver().getFirstName() + " " + ride.getDriver().getLastName())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .settlementDate(payment.getSettlementDate())
                .build();
    }
}