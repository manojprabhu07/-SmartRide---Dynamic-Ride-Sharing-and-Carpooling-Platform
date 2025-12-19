package com.ridesharing.entity;

/**
 * Enum representing different types of ride reminders
 * Based on time difference between booking and ride time
 */
public enum ReminderType {
    THIRTY_MINUTES_BEFORE,  // For bookings made less than 1 hour before ride
    ONE_HOUR_BEFORE,       // For bookings made 1-24 hours before ride
    TWENTY_FOUR_HOURS_BEFORE, // For bookings made more than 24 hours before ride
    ONE_HOUR_BEFORE_FINAL  // Final reminder 1 hour before ride (for 24h+ bookings)
}