package com.ridesharing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service for automatically processing ride reminders
 * Runs periodic tasks to send due reminders and retry failed ones
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    value = "app.reminders.scheduling.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class ReminderSchedulerService {

    private final RideReminderService reminderService;

    /**
     * Process due reminders every 5 minutes
     * This ensures timely delivery of reminders within a reasonable window
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    public void processDueReminders() {
        try {
            log.debug("Starting scheduled processing of due reminders");
            reminderService.processDueReminders();
            log.debug("Completed scheduled processing of due reminders");
        } catch (Exception e) {
            log.error("Error during scheduled processing of due reminders", e);
        }
    }

    /**
     * Retry failed reminders every 30 minutes
     * This gives failed reminders a chance to be sent again
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes = 1,800,000 milliseconds
    public void retryFailedReminders() {
        try {
            log.debug("Starting scheduled retry of failed reminders");
            reminderService.retryFailedReminders();
            log.debug("Completed scheduled retry of failed reminders");
        } catch (Exception e) {
            log.error("Error during scheduled retry of failed reminders", e);
        }
    }

    /**
     * Clean up old reminders daily at 2 AM
     * Remove reminders for rides that have already completed
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    public void cleanupOldReminders() {
        try {
            log.info("Starting scheduled cleanup of old reminders");
            // This would be implemented to clean up reminders for completed/cancelled rides
            // For now, we'll just log the intention
            log.info("Old reminder cleanup would run here (not implemented yet)");
            log.info("Completed scheduled cleanup of old reminders");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of old reminders", e);
        }
    }

    /**
     * Log reminder statistics every hour
     * Helps with monitoring the health of the reminder system
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3,600,000 milliseconds
    public void logReminderStatistics() {
        try {
            var stats = reminderService.getReminderStatistics();
            log.info("Reminder Statistics - Scheduled: {}, Sent: {}, Failed: {}, Cancelled: {}", 
                    stats.get("scheduled"), stats.get("sent"), stats.get("failed"), stats.get("cancelled"));
        } catch (Exception e) {
            log.error("Error logging reminder statistics", e);
        }
    }
}