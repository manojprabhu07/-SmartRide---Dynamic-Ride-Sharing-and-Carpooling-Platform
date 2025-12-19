package com.ridesharing.repository;

import com.ridesharing.entity.RideReminder;
import com.ridesharing.entity.ReminderStatus;
import com.ridesharing.entity.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RideReminder entity
 * Provides custom query methods for reminder management
 */
@Repository
public interface RideReminderRepository extends JpaRepository<RideReminder, Long> {

    /**
     * Find all reminders that are scheduled and due to be sent
     */
    @Query("SELECT r FROM RideReminder r WHERE r.status = :status AND r.scheduledTime <= :currentTime")
    List<RideReminder> findDueReminders(@Param("status") ReminderStatus status, 
                                       @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all reminders for a specific booking
     */
    @Query("SELECT r FROM RideReminder r WHERE r.booking.id = :bookingId ORDER BY r.scheduledTime")
    List<RideReminder> findByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Find reminders by booking ID and type
     */
    @Query("SELECT r FROM RideReminder r WHERE r.booking.id = :bookingId AND r.reminderType = :type")
    Optional<RideReminder> findByBookingIdAndType(@Param("bookingId") Long bookingId, 
                                                  @Param("type") ReminderType type);

    /**
     * Find all failed reminders that can be retried
     */
    @Query("SELECT r FROM RideReminder r WHERE r.status = :status AND r.retryCount < r.maxRetries")
    List<RideReminder> findRetryableReminders(@Param("status") ReminderStatus status);

    /**
     * Find reminders by recipient email
     */
    @Query("SELECT r FROM RideReminder r WHERE r.recipientEmail = :email ORDER BY r.scheduledTime DESC")
    List<RideReminder> findByRecipientEmail(@Param("email") String email);

    /**
     * Find all reminders for a specific passenger (through booking relationship)
     */
    @Query("SELECT r FROM RideReminder r WHERE r.booking.passenger.id = :passengerId ORDER BY r.scheduledTime DESC")
    List<RideReminder> findByPassengerId(@Param("passengerId") Long passengerId);

    /**
     * Find all reminders for rides by a specific driver (through booking-ride relationship)
     */
    @Query("SELECT r FROM RideReminder r WHERE r.booking.ride.driver.id = :driverId ORDER BY r.scheduledTime DESC")
    List<RideReminder> findByDriverId(@Param("driverId") Long driverId);

    /**
     * Count reminders by status
     */
    @Query("SELECT COUNT(r) FROM RideReminder r WHERE r.status = :status")
    Long countByStatus(@Param("status") ReminderStatus status);

    /**
     * Find reminders scheduled between two dates
     */
    @Query("SELECT r FROM RideReminder r WHERE r.scheduledTime BETWEEN :startTime AND :endTime ORDER BY r.scheduledTime")
    List<RideReminder> findScheduledBetween(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * Delete reminders for cancelled bookings
     */
    @Query("DELETE FROM RideReminder r WHERE r.booking.status = com.ridesharing.entity.BookingStatus.CANCELLED")
    void deleteCancelledBookingReminders();

    /**
     * Find overdue reminders (scheduled time has passed but still in SCHEDULED status)
     */
    @Query("SELECT r FROM RideReminder r WHERE r.status = :status AND r.scheduledTime < :currentTime")
    List<RideReminder> findOverdueReminders(@Param("status") ReminderStatus status, 
                                           @Param("currentTime") LocalDateTime currentTime);

    /**
     * Check if a reminder already exists for a booking and type combination
     */
    @Query("SELECT COUNT(r) > 0 FROM RideReminder r WHERE r.booking.id = :bookingId AND r.reminderType = :type")
    boolean existsByBookingIdAndType(@Param("bookingId") Long bookingId, @Param("type") ReminderType type);
}