package com.ridesharing.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class BookingDto {

    @NotNull(message = "Ride ID is required")
    private Long rideId;

    @NotNull(message = "Number of seats is required")
    @Min(value = 1, message = "Must book at least 1 seat")
    @Max(value = 4, message = "Cannot book more than 4 seats at once")
    private Integer seatsBooked;

    @NotBlank(message = "Passenger name is required")
    @Size(max = 100, message = "Passenger name must not exceed 100 characters")
    private String passengerName;

    @NotBlank(message = "Passenger phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String passengerPhone;

    @Size(max = 200, message = "Pickup point must not exceed 200 characters")
    private String pickupPoint;
}