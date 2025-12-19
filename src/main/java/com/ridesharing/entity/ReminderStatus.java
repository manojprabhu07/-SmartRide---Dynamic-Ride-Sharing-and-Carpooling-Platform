package com.ridesharing.entity;

/**
 * Enum representing the status of a ride reminder
 */
public enum ReminderStatus {
    SCHEDULED,  // Reminder is scheduled but not yet sent
    SENT,       // Reminder has been successfully sent
    FAILED,     // Reminder sending failed
    CANCELLED   // Reminder was cancelled (e.g., ride was cancelled)
}