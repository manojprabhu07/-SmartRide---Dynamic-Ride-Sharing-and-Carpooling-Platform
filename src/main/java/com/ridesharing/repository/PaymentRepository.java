package com.ridesharing.repository;

import com.ridesharing.entity.Payment;
import com.ridesharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment Repository - Database operations for Payment entity
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by Razorpay order ID
     */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Find payment by Razorpay payment ID
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Find payment by booking ID
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId")
    Optional<Payment> findByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Get all payments for a passenger (transaction history)
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.passenger.id = :passengerId ORDER BY p.createdAt DESC")
    List<Payment> findByPassengerId(@Param("passengerId") Long passengerId);

    /**
     * Get all payments for a driver (earnings history)
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.ride.driver.id = :driverId ORDER BY p.createdAt DESC")
    List<Payment> findByDriverId(@Param("driverId") Long driverId);

    /**
     * Get payments by status
     */
    List<Payment> findByPaymentStatusOrderByCreatedAtDesc(Payment.PaymentStatus paymentStatus);

    /**
     * Get pending settlements for driver
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.ride.driver.id = :driverId AND p.settlementStatus = com.ridesharing.entity.Payment$SettlementStatus.PENDING AND p.paymentStatus = com.ridesharing.entity.Payment$PaymentStatus.COMPLETED")
    List<Payment> findPendingSettlementsByDriverId(@Param("driverId") Long driverId);

    /**
     * Get payments within date range for passenger
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.passenger.id = :passengerId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findByPassengerIdAndDateRange(@Param("passengerId") Long passengerId, 
                                                @Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Get payments within date range for driver
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.ride.driver.id = :driverId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findByDriverIdAndDateRange(@Param("driverId") Long driverId, 
                                            @Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get total earnings for driver
     */
    @Query("SELECT COALESCE(SUM(p.driverSettlementAmount), 0) FROM Payment p WHERE p.booking.ride.driver.id = :driverId AND p.paymentStatus = com.ridesharing.entity.Payment$PaymentStatus.COMPLETED AND p.settlementStatus = com.ridesharing.entity.Payment$SettlementStatus.COMPLETED")
    Double getTotalEarningsByDriverId(@Param("driverId") Long driverId);

    /**
     * Get total spending for passenger
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.booking.passenger.id = :passengerId AND p.paymentStatus = com.ridesharing.entity.Payment$PaymentStatus.COMPLETED")
    Double getTotalSpendingByPassengerId(@Param("passengerId") Long passengerId);

    /**
     * Get failed payments for retry
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = com.ridesharing.entity.Payment$PaymentStatus.FAILED AND p.createdAt > :afterDate ORDER BY p.createdAt DESC")
    List<Payment> findFailedPaymentsAfterDate(@Param("afterDate") LocalDateTime afterDate);

    /**
     * Count successful payments for user
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.booking.passenger.id = :userId AND p.paymentStatus = com.ridesharing.entity.Payment$PaymentStatus.COMPLETED")
    Long countSuccessfulPaymentsByUserId(@Param("userId") Long userId);

    /**
     * Get recent payments (last 30 days) for dashboard
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :thirtyDaysAgo AND p.paymentStatus = com.ridesharing.entity.Payment$PaymentStatus.COMPLETED ORDER BY p.createdAt DESC")
    List<Payment> findRecentSuccessfulPayments(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
}