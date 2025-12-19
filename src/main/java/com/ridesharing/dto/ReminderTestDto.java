package com.ridesharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for testing ride reminder scenarios
 */
@Data
public class ReminderTestDto {
    private String passengerEmail;
    private String driverName;
    private String driverPhone;
    private String source;
    private String destination;
    private LocalDateTime bookingTime;
    private LocalDateTime rideTime;
    private Integer seatsBooked;
    private String vehicleInfo;
    private String scenario; // Description of test scenario
}