package com.ridesharing.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DriverDetailDto {

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String licenseNumber;

    @NotNull(message = "License expiry date is required")
    @FutureOrPresent(message = "License expiry date cannot be in the past")
    private LocalDate licenseExpiryDate; // This field name

    @NotBlank(message = "Car number is required")
    @Size(max = 20, message = "Car number must not exceed 20 characters")
    private String carNumber;

    @NotBlank(message = "Car model is required")
    @Size(max = 100, message = "Car model must not exceed 100 characters")
    private String carModel;

    @NotBlank(message = "Car color is required")
    @Size(max = 30, message = "Car color must not exceed 30 characters")
    private String carColor;

    @NotNull(message = "Car year is required")
    @Min(value = 1900, message = "Car year must be after 1900")
    @Max(value = 2030, message = "Car year must not exceed 2030")
    private Integer carYear;

    @NotBlank(message = "Insurance number is required")
    @Size(max = 50, message = "Insurance number must not exceed 50 characters")
    private String insuranceNumber;

    @NotNull(message = "Insurance expiry date is required")
    @FutureOrPresent(message = "Insurance expiry date cannot be in the past")
    private LocalDate insuranceExpiryDate; // This field name
}