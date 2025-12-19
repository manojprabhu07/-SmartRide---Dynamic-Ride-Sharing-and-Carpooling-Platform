package com.ridesharing.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for distance calculation response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistanceResponseDto {
    
    /**
     * Distance in kilometers
     */
    private Double distanceKm;
    
    /**
     * Duration in minutes
     */
    private Integer durationMinutes;
    
    /**
     * Human readable distance text (e.g., "12.5 km")
     */
    private String distanceText;
    
    /**
     * Human readable duration text (e.g., "25 mins")
     */
    private String durationText;
    
    /**
     * Calculated fare based on distance
     */
    private BigDecimal calculatedFare;
    
    /**
     * Status of the calculation (SUCCESS, ERROR, etc.)
     */
    private String status;
    
    /**
     * Error message if calculation failed
     */
    private String errorMessage;
    
    /**
     * Constructor for successful response
     */
    public DistanceResponseDto(Double distanceKm, Integer durationMinutes, 
                              String distanceText, String durationText, 
                              BigDecimal calculatedFare) {
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.distanceText = distanceText;
        this.durationText = durationText;
        this.calculatedFare = calculatedFare;
        this.status = "SUCCESS";
    }
    
    /**
     * Constructor for error response
     */
    public DistanceResponseDto(String errorMessage) {
        this.status = "ERROR";
        this.errorMessage = errorMessage;
    }
}