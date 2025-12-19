package com.ridesharing.entity;

public enum BookingStatus {
    PENDING,    // Default status when passenger books a ride
    CONFIRMED,  // Driver confirms the booking
    PAID,       // Payment completed by passenger
    CANCELLED,  // Driver or passenger cancels
    COMPLETED   // Ride is completed
}