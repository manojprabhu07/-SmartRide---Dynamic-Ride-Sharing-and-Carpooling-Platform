package com.ridesharing.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for fare calculation request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareCalculationRequest {
    
    @NotBlank(message = "Source is required")
    private String source;
    
    @NotBlank(message = "Destination is required")
    private String destination;
}