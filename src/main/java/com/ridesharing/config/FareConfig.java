package com.ridesharing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration class for fare calculation settings
 */
@Component
@ConfigurationProperties(prefix = "app.fare")
@Data
public class FareConfig {
    
    /**
     * Base fare amount (minimum charge)
     * This is the starting fare before distance calculation
     */
    private BigDecimal base = new BigDecimal("50.00");
    
    /**
     * Rate per kilometer
     * This amount is multiplied by the distance in km
     */
    private BigDecimal ratePerKm = new BigDecimal("10.00");
    
    /**
     * Currency code (INR, USD, EUR, etc.)
     */
    private String currency = "INR";
    
    /**
     * Maximum fare limit (optional safety measure)
     */
    private BigDecimal maxFare = new BigDecimal("2000.00");
    
    /**
     * Minimum fare limit (cannot be less than base fare)
     */
    private BigDecimal minFare = new BigDecimal("50.00");
}