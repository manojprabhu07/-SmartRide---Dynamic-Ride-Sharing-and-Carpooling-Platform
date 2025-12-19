package com.ridesharing.service;

import com.ridesharing.entity.*;
import com.ridesharing.repository.RideReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing ride reminder notifications
 * Handles scheduling and sending of email reminders based on booking-to-ride time differences
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideReminderService {

    private final RideReminderRepository reminderRepository;
    private final EmailService emailService;

    /**
     * Schedule reminders for a confirmed booking
     * Logic:
     * - If booking time < 1 hour before ride: send 30min reminder
     * - If booking time 1-24 hours before ride: send 1h reminder  
     * - If booking time > 24 hours before ride: send 24h + 1h reminders
     */
    @Transactional
    public void scheduleRemindersForBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.debug("Booking {} is not confirmed, skipping reminder scheduling", booking.getId());
            return;
        }

        try {
            LocalDateTime bookingTime = booking.getBookingDate();
            LocalDateTime rideTime = booking.getRide().getDepartureDate();
            
            // Calculate time difference between booking and ride
            Duration timeDifference = Duration.between(bookingTime, rideTime);
            long hoursDifference = timeDifference.toHours();
            long minutesDifference = timeDifference.toMinutes();
            
            log.info("Scheduling reminders for booking {}: {} hours ({} minutes) difference. Booking: {}, Ride: {}, Current: {}", 
                    booking.getId(), hoursDifference, minutesDifference, bookingTime, rideTime, LocalDateTime.now());

            // Clear any existing reminders for this booking
            List<RideReminder> existingReminders = reminderRepository.findByBookingId(booking.getId());
            reminderRepository.deleteAll(existingReminders);

            List<RideReminder> remindersToCreate = new ArrayList<>();

            if (hoursDifference < 1) {
                // Less than 1 hour: send 30-minute reminder
                LocalDateTime reminderTime = rideTime.minusMinutes(30);
                // For short bookings, allow scheduling even if reminder time is close to now
                if (reminderTime.isAfter(LocalDateTime.now().minusMinutes(5))) {
                    RideReminder reminder = createReminder(booking, ReminderType.THIRTY_MINUTES_BEFORE, reminderTime);
                    remindersToCreate.add(reminder);
                    log.info("Scheduling 30-minute reminder for booking {} at {}", booking.getId(), reminderTime);
                } else {
                    log.warn("30-minute reminder time {} is too close to current time for booking {}", reminderTime, booking.getId());
                }
            } else if (hoursDifference <= 24) {
                // 1-24 hours: send 1-hour reminder
                LocalDateTime reminderTime = rideTime.minusHours(1);
                if (reminderTime.isAfter(LocalDateTime.now())) {
                    RideReminder reminder = createReminder(booking, ReminderType.ONE_HOUR_BEFORE, reminderTime);
                    remindersToCreate.add(reminder);
                }
            } else {
                // More than 24 hours: send 24-hour reminder AND 1-hour reminder
                LocalDateTime twentyFourHourReminderTime = rideTime.minusHours(24);
                LocalDateTime oneHourReminderTime = rideTime.minusHours(1);
                
                if (twentyFourHourReminderTime.isAfter(LocalDateTime.now())) {
                    RideReminder reminder24h = createReminder(booking, ReminderType.TWENTY_FOUR_HOURS_BEFORE, twentyFourHourReminderTime);
                    remindersToCreate.add(reminder24h);
                }
                
                if (oneHourReminderTime.isAfter(LocalDateTime.now())) {
                    RideReminder reminder1h = createReminder(booking, ReminderType.ONE_HOUR_BEFORE_FINAL, oneHourReminderTime);
                    remindersToCreate.add(reminder1h);
                }
            }

            // Save all reminders
            if (!remindersToCreate.isEmpty()) {
                reminderRepository.saveAll(remindersToCreate);
                log.info("Created {} reminders for booking {}", remindersToCreate.size(), booking.getId());
            } else {
                log.info("No reminders needed for booking {} (ride time is too soon)", booking.getId());
            }

        } catch (Exception e) {
            log.error("Error scheduling reminders for booking {}", booking.getId(), e);
            throw new RuntimeException("Failed to schedule reminders", e);
        }
    }

    /**
     * Create a single reminder instance
     */
    private RideReminder createReminder(Booking booking, ReminderType type, LocalDateTime scheduledTime) {
        RideReminder reminder = new RideReminder();
        reminder.setBooking(booking);
        reminder.setReminderType(type);
        reminder.setScheduledTime(scheduledTime);
        reminder.setRecipientEmail(booking.getPassenger().getEmail());
        reminder.setMessage(generateReminderMessage(booking, type));
        return reminder;
    }

    /**
     * Generate appropriate reminder message based on type
     */
    private String generateReminderMessage(Booking booking, ReminderType type) {
        Ride ride = booking.getRide();
        String timeInfo = switch (type) {
            case THIRTY_MINUTES_BEFORE -> "in 30 minutes";
            case ONE_HOUR_BEFORE -> "in 1 hour";
            case TWENTY_FOUR_HOURS_BEFORE -> "in 24 hours";
            case ONE_HOUR_BEFORE_FINAL -> "in 1 hour";
        };
        
        return String.format("Your ride from %s to %s is scheduled %s. Please be ready!",
                ride.getSource(), ride.getDestination(), timeInfo);
    }

    /**
     * Process all due reminders
     */
    @Transactional
    public void processDueReminders() {
        try {
            List<RideReminder> dueReminders = reminderRepository.findDueReminders(
                    ReminderStatus.SCHEDULED, LocalDateTime.now());
            
            log.info("Found {} due reminders to process", dueReminders.size());
            
            for (RideReminder reminder : dueReminders) {
                try {
                    sendReminderEmail(reminder);
                    reminder.markAsSent();
                    reminderRepository.save(reminder);
                    log.info("Successfully sent reminder {} for booking {}", 
                            reminder.getId(), reminder.getBooking().getId());
                } catch (Exception e) {
                    log.error("Failed to send reminder {} for booking {}", 
                            reminder.getId(), reminder.getBooking().getId(), e);
                    reminder.markAsFailed(e.getMessage());
                    reminderRepository.save(reminder);
                }
            }
        } catch (Exception e) {
            log.error("Error processing due reminders", e);
        }
    }

    /**
     * Retry failed reminders
     */
    @Transactional
    public void retryFailedReminders() {
        try {
            List<RideReminder> failedReminders = reminderRepository.findRetryableReminders(ReminderStatus.FAILED);
            
            log.info("Found {} failed reminders to retry", failedReminders.size());
            
            for (RideReminder reminder : failedReminders) {
                try {
                    sendReminderEmail(reminder);
                    reminder.markAsSent();
                    reminderRepository.save(reminder);
                    log.info("Successfully retried reminder {} for booking {}", 
                            reminder.getId(), reminder.getBooking().getId());
                } catch (Exception e) {
                    log.error("Failed to retry reminder {} for booking {}", 
                            reminder.getId(), reminder.getBooking().getId(), e);
                    reminder.markAsFailed(e.getMessage());
                    reminderRepository.save(reminder);
                }
            }
        } catch (Exception e) {
            log.error("Error retrying failed reminders", e);
        }
    }

    /**
     * Send reminder email using the existing EmailService
     */
    private void sendReminderEmail(RideReminder reminder) {
        Booking booking = reminder.getBooking();
        Ride ride = booking.getRide();
        User passenger = booking.getPassenger();
        User driver = ride.getDriver();

        String subject = getReminderSubject(reminder.getReminderType());
        Map<String, Object> templateModel = createReminderEmailModel(reminder, passenger, booking, ride, driver);
        
        try {
            // Use the new template-based email method
            emailService.sendRideReminderEmail(reminder.getRecipientEmail(), subject, templateModel);
        } catch (Exception e) {
            log.error("Failed to send reminder email", e);
            throw e;
        }
    }

    /**
     * Get appropriate subject line for reminder type
     */
    private String getReminderSubject(ReminderType type) {
        return switch (type) {
            case THIRTY_MINUTES_BEFORE -> "Ride Reminder: Your ride starts in 30 minutes - SmartRide";
            case ONE_HOUR_BEFORE -> "Ride Reminder: Your ride starts in 1 hour - SmartRide";
            case TWENTY_FOUR_HOURS_BEFORE -> "Ride Reminder: Your ride is tomorrow - SmartRide";
            case ONE_HOUR_BEFORE_FINAL -> "Final Reminder: Your ride starts in 1 hour - SmartRide";
        };
    }

    /**
     * Create email template model for reminder
     */
    private Map<String, Object> createReminderEmailModel(RideReminder reminder, User passenger, 
                                                        Booking booking, Ride ride, User driver) {
        Map<String, Object> model = new HashMap<>();
        
        // Passenger details
        model.put("passengerName", passenger.getFirstName() + " " + passenger.getLastName());
        
        // Ride details
        model.put("source", ride.getSource());
        model.put("destination", ride.getDestination());
        model.put("departureDate", ride.getDepartureDate().toLocalDate().toString());
        model.put("departureTime", ride.getDepartureDate().toLocalTime().toString());
        model.put("seatsBooked", booking.getSeatsBooked());
        
        // Driver details
        model.put("driverName", driver.getFirstName() + " " + driver.getLastName());
        model.put("driverPhone", driver.getPhoneNumber());
        model.put("vehicleInfo", ride.getVehicleNumber() + " (" + ride.getVehicleType() + ")");
        
        // Reminder specific
        model.put("reminderType", reminder.getReminderType().toString());
        model.put("message", reminder.getMessage());
        
        return model;
    }

    /**
     * Format reminder email content as simple text
     */
    private String formatReminderEmailContent(Map<String, Object> model) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(model.get("passengerName")).append(",\n\n");
        content.append(model.get("message")).append("\n\n");
        content.append("Ride Details:\n");
        content.append("From: ").append(model.get("source")).append("\n");
        content.append("To: ").append(model.get("destination")).append("\n");
        content.append("Date: ").append(model.get("departureDate")).append("\n");
        content.append("Time: ").append(model.get("departureTime")).append("\n");
        content.append("Seats: ").append(model.get("seatsBooked")).append("\n\n");
        content.append("Driver Details:\n");
        content.append("Name: ").append(model.get("driverName")).append("\n");
        content.append("Phone: ").append(model.get("driverPhone")).append("\n");
        content.append("Vehicle: ").append(model.get("vehicleInfo")).append("\n\n");
        content.append("Thank you for using SmartRide!\n");
        content.append("Safe travels!");
        
        return content.toString();
    }

    /**
     * Cancel all reminders for a booking (e.g., when booking is cancelled)
     */
    @Transactional
    public void cancelRemindersForBooking(Long bookingId) {
        try {
            List<RideReminder> reminders = reminderRepository.findByBookingId(bookingId);
            for (RideReminder reminder : reminders) {
                if (reminder.getStatus() == ReminderStatus.SCHEDULED) {
                    reminder.markAsCancelled();
                }
            }
            reminderRepository.saveAll(reminders);
            log.info("Cancelled {} reminders for booking {}", reminders.size(), bookingId);
        } catch (Exception e) {
            log.error("Error cancelling reminders for booking {}", bookingId, e);
        }
    }

    /**
     * Get reminders for a specific booking
     */
    public List<RideReminder> getRemindersForBooking(Long bookingId) {
        return reminderRepository.findByBookingId(bookingId);
    }

    /**
     * Get reminders for a specific passenger
     */
    public List<RideReminder> getRemindersForPassenger(Long passengerId) {
        return reminderRepository.findByPassengerId(passengerId);
    }

    /**
     * Get reminder statistics
     */
    public Map<String, Long> getReminderStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("scheduled", reminderRepository.countByStatus(ReminderStatus.SCHEDULED));
        stats.put("sent", reminderRepository.countByStatus(ReminderStatus.SENT));
        stats.put("failed", reminderRepository.countByStatus(ReminderStatus.FAILED));
        stats.put("cancelled", reminderRepository.countByStatus(ReminderStatus.CANCELLED));
        return stats;
    }

    /**
     * Send test email for debugging
     */
    public void sendTestEmail(String to, String subject, String content) {
        try {
            emailService.sendSimpleEmail(to, subject, content);
            log.info("Test email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", to, e);
            throw e;
        }
    }
}