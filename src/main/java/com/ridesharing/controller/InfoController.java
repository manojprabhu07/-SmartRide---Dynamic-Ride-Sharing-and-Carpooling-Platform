package com.ridesharing.controller;

import com.ridesharing.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Info controller for supported cities and distance calculation details
 */
@RestController
@RequestMapping("/api/info")
@RequiredArgsConstructor
public class InfoController {
    
    /**
     * Get list of supported cities (for which we have pre-calculated distances)
     */
    @GetMapping("/supported-cities")
    public ResponseEntity<ApiResponse> getSupportedCities() {
        String[] cities = {
            "Delhi", "Mumbai", "Kolkata", "Chennai", "Bangalore", 
            "Hyderabad", "Pune", "Ahmedabad", "Jaipur", "Lucknow",
            "Kanpur", "Agra", "Goa", "Bhubaneswar", "Kochi", "Mysore"
        };
        
        return ResponseEntity.ok(new ApiResponse(
            "SUCCESS",
            "Cities with pre-calculated distances. Other cities use coordinate-based calculation.",
            cities
        ));
    }
    
    /**
     * Get information about the distance calculation methods
     */
    @GetMapping("/calculation-methods")
    public ResponseEntity<ApiResponse> getCalculationMethods() {
        String[] methods = {
            "Method 1: OpenStreetMap Nominatim + Haversine Formula (FREE - No API key)",
            "Method 2: Coordinate-based calculation for any city worldwide", 
            "Method 3: Pre-calculated database for major Indian cities (instant lookup)",
            "Formula: Fare = Base Fare (₹50) + (Distance × Rate per KM (₹10))"
        };
        
        return ResponseEntity.ok(new ApiResponse(
            "SUCCESS",
            "Distance calculation methods used by the system",
            methods
        ));
    }
}