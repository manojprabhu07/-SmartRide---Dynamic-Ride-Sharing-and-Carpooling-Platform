package com.ridesharing.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class RidePostDto {

    @NotBlank(message = "Source is required")
    @Size(max = 100, message = "Source must not exceed 100 characters")
    private String source;

    @NotBlank(message = "Destination is required")
    @Size(max = 100, message = "Destination must not exceed 100 characters")
    private String destination;

    @NotNull(message = "Departure date is required")
    @Future(message = "Departure date must be in the future")
    private LocalDateTime departureDate;

    @NotNull(message = "Available seats is required")
    @Min(value = 1, message = "Available seats must be at least 1")
    @Max(value = 8, message = "Available seats cannot exceed 8")
    private Integer availableSeats;

    @NotNull(message = "Price per seat is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per seat must be greater than 0")
    @DecimalMax(value = "10000.0", message = "Price per seat cannot exceed 10000")
    private BigDecimal pricePerSeat;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}