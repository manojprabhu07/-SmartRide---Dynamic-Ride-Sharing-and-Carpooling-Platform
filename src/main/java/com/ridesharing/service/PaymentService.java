package com.ridesharing.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.ridesharing.dto.PaymentOrderRequest;
import com.ridesharing.dto.PaymentOrderResponse;
import com.ridesharing.dto.PaymentVerificationRequest;
import com.ridesharing.dto.PaymentHistoryResponse;
import com.ridesharing.entity.Booking;
import com.ridesharing.entity.BookingStatus;
import com.ridesharing.entity.Payment;
import com.ridesharing.entity.Ride;
import com.ridesharing.exception.PaymentException;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment Service - Handles all payment operations using Razorpay
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${razorpay.company.name:SmartRide}")
    private String companyName;

    // Platform commission percentage
    @Value("${app.platform.commission:10.0}")
    private BigDecimal platformCommission;

    /**
     * Create Razorpay order for booking payment
     */
    @Transactional
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request) {
        try {
            log.info("🚀 Creating payment order for booking: {}", request.getBookingId());
            
            // Validate Razorpay configuration
            if (razorpayKeyId == null || razorpayKeyId.contains("YOUR_KEY_ID") || razorpayKeySecret == null || razorpayKeySecret.contains("YOUR_SECRET")) {
                throw new PaymentException("Razorpay is not configured properly. Please set valid key.id and key.secret in application.properties");
            }
            
            // Validate booking exists and is confirmed
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new PaymentException("Booking not found with ID: " + request.getBookingId()));

            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                throw new PaymentException("Payment can only be made for confirmed bookings");
            }

            // Check if payment already exists for this booking
            if (paymentRepository.findByBookingId(booking.getId()).isPresent()) {
                throw new PaymentException("Payment already exists for this booking");
            }

            // Initialize Razorpay client
            log.info("🔑 Initializing Razorpay with Key ID: {}...", razorpayKeyId.substring(0, Math.min(10, razorpayKeyId.length())));
            
            RazorpayClient razorpay;
            try {
                razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                log.info("✅ Razorpay client initialized successfully");
            } catch (Exception e) {
                log.error("❌ Failed to initialize Razorpay client: {}", e.getMessage());
                throw new PaymentException("Failed to initialize payment gateway: " + e.getMessage());
            }

            // Convert amount to paise (Razorpay expects amount in smallest currency unit)
            BigDecimal amountInPaise = booking.getTotalAmount().multiply(BigDecimal.valueOf(100));

            // Create order in Razorpay
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise.intValue());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "booking_" + booking.getId() + "_" + System.currentTimeMillis());
            orderRequest.put("notes", new JSONObject()
                    .put("booking_id", booking.getId())
                    .put("passenger_id", booking.getPassenger().getId())
                    .put("driver_id", booking.getRide().getDriver().getId())
                    .put("route", booking.getRide().getSource() + " to " + booking.getRide().getDestination()));

            log.info("📝 Creating order with amount: ₹{} (Booking ID: {})", booking.getTotalAmount(), booking.getId());
            
            Order razorpayOrder;
            try {
                razorpayOrder = razorpay.orders.create(orderRequest);
                log.info("✅ Razorpay order created: {}", razorpayOrder.get("id").toString());
            } catch (Exception e) {
                log.error("❌ Failed to create Razorpay order: {}", e.getMessage(), e);
                throw new PaymentException("Failed to create payment order: " + e.getMessage());
            }

            // Create payment record in database
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setRazorpayOrderId(razorpayOrder.get("id"));
            payment.setAmount(booking.getTotalAmount());
            payment.setCurrency(currency);
            payment.setPaymentStatus(Payment.PaymentStatus.CREATED);
            payment.setReceiptNumber(orderRequest.getString("receipt"));
            payment.setDescription("Payment for ride from " + booking.getRide().getSource() + " to " + booking.getRide().getDestination());
            
            // Calculate settlement amounts
            payment.calculateSettlement(platformCommission);

            payment = paymentRepository.save(payment);

            // Return order response for frontend
            return PaymentOrderResponse.builder()
                    .orderId(razorpayOrder.get("id"))
                    .amount(booking.getTotalAmount())
                    .currency(currency)
                    .keyId(razorpayKeyId)
                    .companyName(companyName)
                    .description(payment.getDescription())
                    .contactEmail(booking.getPassenger().getEmail())
                    .contactPhone(booking.getPassenger().getPhoneNumber())
                    .bookingId(booking.getId())
                    .paymentId(payment.getId())
                    .build();

        } catch (Exception e) {
            log.error("Payment order creation failed", e);
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    /**
     * Verify Razorpay payment signature and update payment status
     */
    @Transactional
    public boolean verifyPayment(PaymentVerificationRequest request) {
        try {
            // Find payment by order ID
            Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new PaymentException("Payment not found for order ID: " + request.getRazorpayOrderId()));

            // Verify signature using Razorpay Utils
            String generatedSignature = Utils.getHash(request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(), razorpayKeySecret);

            if (generatedSignature.equals(request.getRazorpaySignature())) {
                // Signature is valid - update payment
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                payment.setRazorpaySignature(request.getRazorpaySignature());
                payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaymentMethod(request.getPaymentMethod());
                payment.setGatewayResponse(request.getGatewayResponse());

                paymentRepository.save(payment);

                // Update booking status to PAID
                Booking booking = payment.getBooking();
                booking.setStatus(BookingStatus.PAID);
                bookingRepository.save(booking);

                log.info("Payment verified successfully for order: {}", request.getRazorpayOrderId());
                return true;

            } else {
                // Signature verification failed
                payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                payment.setGatewayResponse("Signature verification failed");
                paymentRepository.save(payment);

                log.warn("Payment signature verification failed for order: {}", request.getRazorpayOrderId());
                return false;
            }

        } catch (Exception e) {
            log.error("Payment verification failed", e);
            throw new PaymentException("Payment verification failed: " + e.getMessage());
        }
    }

    /**
     * Get payment history for passenger
     */
    public List<PaymentHistoryResponse> getPassengerPaymentHistory(Long passengerId) {
        List<Payment> payments = paymentRepository.findByPassengerId(passengerId);
        return payments.stream()
                .map(this::mapToPaymentHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get payment history for driver (earnings)
     */
    public List<PaymentHistoryResponse> getDriverPaymentHistory(Long driverId) {
        List<Payment> payments = paymentRepository.findByDriverId(driverId);
        return payments.stream()
                .map(this::mapToPaymentHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get payment history within date range
     */
    public List<PaymentHistoryResponse> getPaymentHistoryByDateRange(Long userId, boolean isDriver, 
                                                                    LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments;
        if (isDriver) {
            payments = paymentRepository.findByDriverIdAndDateRange(userId, startDate, endDate);
        } else {
            payments = paymentRepository.findByPassengerIdAndDateRange(userId, startDate, endDate);
        }
        
        return payments.stream()
                .map(this::mapToPaymentHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Release payment to driver after ride completion
     * This is called automatically when driver marks ride as complete
     */
    @Transactional
    public void releasePaymentToDriver(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentException("Payment not found for booking: " + bookingId));

        if (payment.getPaymentStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new PaymentException("Payment is not completed yet");
        }

        if (payment.getSettlementStatus() == Payment.SettlementStatus.COMPLETED) {
            throw new PaymentException("Payment already settled to driver");
        }

        // Mark settlement as completed
        payment.setSettlementStatus(Payment.SettlementStatus.COMPLETED);
        payment.setSettlementDate(LocalDateTime.now());
        
        paymentRepository.save(payment);

        // Get driver info
        Booking booking = payment.getBooking();
        Long driverId = booking.getRide().getDriver().getId();
        BigDecimal driverEarnings = payment.getDriverSettlementAmount();

        log.info("Payment released to driver {} for booking: {}, Amount: ₹{}", 
                driverId, bookingId, driverEarnings);

        // TODO: Integrate with wallet service to add money to driver wallet
        // walletService.addEarningsToWallet(driverId, driverEarnings, "Ride completion: " + booking.getId());
        
        // TODO: Send notification to driver about payment received
        // notificationService.notifyDriverPaymentReceived(driverId, driverEarnings);
    }

    /**
     * Auto-settle payment when ride is marked complete
     * This method is called from RideService when driver completes ride
     */
    @Transactional
    public void autoSettlePaymentOnRideComplete(Long bookingId) {
        try {
            releasePaymentToDriver(bookingId);
            log.info("Auto-settlement successful for booking: {}", bookingId);
        } catch (PaymentException e) {
            log.warn("Auto-settlement failed for booking: {} - {}", bookingId, e.getMessage());
            // Don't throw exception to avoid blocking ride completion
            // Payment can be settled manually later
        }
    }

    /**
     * Get total earnings for driver
     */
    public BigDecimal getDriverTotalEarnings(Long driverId) {
        Double total = paymentRepository.getTotalEarningsByDriverId(driverId);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    /**
     * Get total spending for passenger
     */
    public BigDecimal getPassengerTotalSpending(Long passengerId) {
        Double total = paymentRepository.getTotalSpendingByPassengerId(passengerId);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    /**
     * Handle payment failure
     */
    @Transactional
    public void handlePaymentFailure(String razorpayOrderId, String reason) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new PaymentException("Payment not found for order: " + razorpayOrderId));

        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setGatewayResponse("Payment failed: " + reason);
        
        paymentRepository.save(payment);

        // Update booking status back to CONFIRMED
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.warn("Payment failed for order: {}, Reason: {}", razorpayOrderId, reason);
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
                .paymentMethod(payment.getPaymentMethod())
                .receiptNumber(payment.getReceiptNumber())
                .description(payment.getDescription())
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