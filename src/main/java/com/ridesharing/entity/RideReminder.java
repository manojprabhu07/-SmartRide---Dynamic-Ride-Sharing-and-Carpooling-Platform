package com.ridesharing.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing ride reminder notifications
 * Stores information about scheduled email reminders for confirmed rides
 */
@Entity
@Table(name = "ride_reminders")
@Data
@EqualsAndHashCode(exclude = {"booking"})
@ToString(exclude = {"booking"})
public class RideReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationship with Booking (Many reminders per booking possible)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Type of reminder based on time difference
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false)
    private ReminderType reminderType;

    // When the reminder should be sent
    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    // Current status of the reminder
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReminderStatus status = ReminderStatus.SCHEDULED;

    // Notification channel (email, SMS, etc.)
    @Column(name = "notification_channel", nullable = false, length = 20)
    private String notificationChannel = "EMAIL";

    // Email address to send reminder to
    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    // Optional message content for the reminder
    @Column(name = "message", length = 500)
    private String message;

    // When the reminder was actually sent
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // Error message if sending failed
    @Column(name = "error_message", length = 255)
    private String errorMessage;

    // Number of retry attempts
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    // Maximum number of retry attempts allowed
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this reminder can be retried
     */
    public boolean canRetry() {
        return this.status == ReminderStatus.FAILED && 
               this.retryCount < this.maxRetries;
    }

    /**
     * Mark reminder as sent successfully
     */
    public void markAsSent() {
        this.status = ReminderStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * Mark reminder as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = ReminderStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * Mark reminder as cancelled
     */
    public void markAsCancelled() {
        this.status = ReminderStatus.CANCELLED;
    }
}