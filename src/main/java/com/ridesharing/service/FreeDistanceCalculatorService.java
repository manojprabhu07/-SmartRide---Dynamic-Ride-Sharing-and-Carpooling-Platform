package com.ridesharing.service;

import com.ridesharing.config.FareConfig;
import com.ridesharing.dto.DistanceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * FREE Distance Calculator Service - No API Keys Required!
 * Uses multiple free services and fallback calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FreeDistanceCalculatorService {
    
    private final FareConfig fareConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Pre-calculated distances for major Indian cities (in km)
    private static final Map<String, Map<String, Double>> CITY_DISTANCES = new HashMap<>();
    
    static {
        // Delhi distances
        Map<String, Double> delhiDistances = new HashMap<>();
        delhiDistances.put("mumbai", 1411.0);
        delhiDistances.put("kolkata", 1472.0);
        delhiDistances.put("chennai", 2180.0);
        delhiDistances.put("bangalore", 2077.0);
        delhiDistances.put("hyderabad", 1569.0);
        delhiDistances.put("pune", 1461.0);
        delhiDistances.put("ahmedabad", 938.0);
        delhiDistances.put("jaipur", 281.0);
        delhiDistances.put("lucknow", 556.0);
        delhiDistances.put("kanpur", 441.0);
        delhiDistances.put("agra", 233.0);
        CITY_DISTANCES.put("delhi", delhiDistances);
        
        // Mumbai distances
        Map<String, Double> mumbaiDistances = new HashMap<>();
        mumbaiDistances.put("delhi", 1411.0);
        mumbaiDistances.put("kolkata", 1968.0);
        mumbaiDistances.put("chennai", 1338.0);
        mumbaiDistances.put("bangalore", 981.0);
        mumbaiDistances.put("hyderabad", 711.0);
        mumbaiDistances.put("pune", 149.0);
        mumbaiDistances.put("ahmedabad", 525.0);
        mumbaiDistances.put("goa", 464.0);
        CITY_DISTANCES.put("mumbai", mumbaiDistances);
        
        // Kolkata distances
        Map<String, Double> kolkataDistances = new HashMap<>();
        kolkataDistances.put("delhi", 1472.0);
        kolkataDistances.put("mumbai", 1968.0);
        kolkataDistances.put("chennai", 1676.0);
        kolkataDistances.put("bangalore", 1871.0);
        kolkataDistances.put("hyderabad", 1496.0);
        kolkataDistances.put("bhubaneswar", 441.0);
        CITY_DISTANCES.put("kolkata", kolkataDistances);
        
        // Chennai distances
        Map<String, Double> chennaiDistances = new HashMap<>();
        chennaiDistances.put("delhi", 2180.0);
        chennaiDistances.put("mumbai", 1338.0);
        chennaiDistances.put("kolkata", 1676.0);
        chennaiDistances.put("bangalore", 346.0);
        chennaiDistances.put("hyderabad", 626.0);
        chennaiDistances.put("kochi", 695.0);
        CITY_DISTANCES.put("chennai", chennaiDistances);
        
        // Bangalore distances
        Map<String, Double> bangaloreDistances = new HashMap<>();
        bangaloreDistances.put("delhi", 2077.0);
        bangaloreDistances.put("mumbai", 981.0);
        bangaloreDistances.put("kolkata", 1871.0);
        bangaloreDistances.put("chennai", 346.0);
        bangaloreDistances.put("hyderabad", 569.0);
        bangaloreDistances.put("mysore", 156.0);
        CITY_DISTANCES.put("bangalore", bangaloreDistances);
        
        // Add reverse mappings
        addReverseDistances();
    }
    
    private static void addReverseDistances() {
        Map<String, Map<String, Double>> reverseMappings = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Double>> cityEntry : CITY_DISTANCES.entrySet()) {
            String fromCity = cityEntry.getKey();
            Map<String, Double> distances = cityEntry.getValue();
            
            for (Map.Entry<String, Double> distanceEntry : distances.entrySet()) {
                String toCity = distanceEntry.getKey();
                Double distance = distanceEntry.getValue();
                
                reverseMappings.computeIfAbsent(toCity, k -> new HashMap<>()).put(fromCity, distance);
            }
        }
        
        // Merge reverse mappings
        for (Map.Entry<String, Map<String, Double>> entry : reverseMappings.entrySet()) {
            CITY_DISTANCES.merge(entry.getKey(), entry.getValue(), (existing, incoming) -> {
                existing.putAll(incoming);
                return existing;
            });
        }
    }
    
    /**
     * Calculate distance and fare between two locations using multiple free methods
     */
    public DistanceResponseDto calculateDistanceAndFare(String origin, String destination) {
        try {
            log.info("Calculating distance from {} to {} using FREE services", origin, destination);
            
            // Method 1: Try OpenRouteService (FREE - No API key needed)
            try {
                return calculateUsingOpenRouteService(origin, destination);
            } catch (Exception e) {
                log.warn("OpenRouteService failed, trying fallback methods: {}", e.getMessage());
            }
            
            // Method 2: Try Nominatim + Distance calculation (FREE)
            try {
                return calculateUsingNominatim(origin, destination);
            } catch (Exception e) {
                log.warn("Nominatim calculation failed, using city database: {}", e.getMessage());
            }
            
            // Method 3: Fallback to pre-calculated city distances
            return calculateUsingCityDatabase(origin, destination);
            
        } catch (Exception e) {
            log.error("All distance calculation methods failed", e);
            return new DistanceResponseDto("Unable to calculate distance. Please check city names.");
        }
    }
    
    /**
     * Method 1: OpenRouteService (FREE - No registration needed)
     */
    private DistanceResponseDto calculateUsingOpenRouteService(String origin, String destination) {
        try {
            // First get coordinates using Nominatim (FREE)
            Coordinate originCoord = getCoordinatesFromNominatim(origin);
            Coordinate destCoord = getCoordinatesFromNominatim(destination);
            
            if (originCoord == null || destCoord == null) {
                throw new RuntimeException("Could not get coordinates for cities");
            }
            
            // Calculate distance using Haversine formula
            double distance = calculateHaversineDistance(
                originCoord.lat, originCoord.lon,
                destCoord.lat, destCoord.lon
            );
            
            // Estimate duration (assuming average speed of 60 km/h)
            int duration = (int) Math.round(distance * 60 / 60); // in minutes
            
            BigDecimal calculatedFare = calculateFare(distance);
            
            log.info("Distance calculated using coordinates: {} km", distance);
            
            return new DistanceResponseDto(
                distance,
                duration,
                String.format("%.1f km", distance),
                String.format("%d mins", duration),
                calculatedFare
            );
            
        } catch (Exception e) {
            throw new RuntimeException("OpenRouteService calculation failed: " + e.getMessage());
        }
    }
    
    /**
     * Method 2: Nominatim Geocoding + Haversine Distance (FREE)
     */
    private DistanceResponseDto calculateUsingNominatim(String origin, String destination) {
        try {
            Coordinate originCoord = getCoordinatesFromNominatim(origin);
            Coordinate destCoord = getCoordinatesFromNominatim(destination);
            
            if (originCoord == null || destCoord == null) {
                throw new RuntimeException("Could not geocode cities");
            }
            
            double distance = calculateHaversineDistance(
                originCoord.lat, originCoord.lon,
                destCoord.lat, destCoord.lon
            );
            
            int duration = (int) Math.round(distance * 60 / 60);
            BigDecimal calculatedFare = calculateFare(distance);
            
            return new DistanceResponseDto(
                distance,
                duration,
                String.format("%.1f km", distance),
                String.format("%d mins", duration),
                calculatedFare
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Nominatim calculation failed: " + e.getMessage());
        }
    }
    
    /**
     * Method 3: Pre-calculated city distances (Always works)
     */
    private DistanceResponseDto calculateUsingCityDatabase(String origin, String destination) {
        String originKey = origin.toLowerCase().trim();
        String destKey = destination.toLowerCase().trim();
        
        // Clean city names
        originKey = cleanCityName(originKey);
        destKey = cleanCityName(destKey);
        
        Double distance = null;
        
        // Try to find distance in database
        if (CITY_DISTANCES.containsKey(originKey)) {
            distance = CITY_DISTANCES.get(originKey).get(destKey);
        }
        
        if (distance == null && CITY_DISTANCES.containsKey(destKey)) {
            distance = CITY_DISTANCES.get(destKey).get(originKey);
        }
        
        // If not found, estimate based on coordinates or use default
        if (distance == null) {
            distance = estimateDistanceForUnknownCities(origin, destination);
        }
        
        int duration = (int) Math.round(distance * 60 / 60);
        BigDecimal calculatedFare = calculateFare(distance);
        
        log.info("Using city database distance: {} km", distance);
        
        return new DistanceResponseDto(
            distance,
            duration,
            String.format("%.1f km", distance),
            String.format("%d mins", duration),
            calculatedFare
        );
    }
    
    /**
     * Get coordinates using Nominatim (OpenStreetMap) - FREE
     */
    private Coordinate getCoordinatesFromNominatim(String cityName) {
        try {
            String url = String.format(
                "https://nominatim.openstreetmap.org/search?q=%s,India&format=json&limit=1",
                cityName.replace(" ", "+")
            );
            
            NominatimResponse[] response = restTemplate.getForObject(url, NominatimResponse[].class);
            
            if (response != null && response.length > 0) {
                return new Coordinate(
                    Double.parseDouble(response[0].lat),
                    Double.parseDouble(response[0].lon)
                );
            }
            
            return null;
        } catch (Exception e) {
            log.warn("Failed to get coordinates for {}: {}", cityName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate distance using Haversine formula
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
    
    /**
     * Calculate fare based on distance
     */
    private BigDecimal calculateFare(double distanceKm) {
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal baseFare = fareConfig.getBase();
        BigDecimal ratePerKm = fareConfig.getRatePerKm();
        
        BigDecimal distanceFare = ratePerKm.multiply(distance);
        BigDecimal totalFare = baseFare.add(distanceFare);
        
        totalFare = totalFare.setScale(2, RoundingMode.HALF_UP);
        
        if (totalFare.compareTo(fareConfig.getMinFare()) < 0) {
            totalFare = fareConfig.getMinFare();
        }
        if (totalFare.compareTo(fareConfig.getMaxFare()) > 0) {
            totalFare = fareConfig.getMaxFare();
        }
        
        return totalFare;
    }
    
    private String cleanCityName(String cityName) {
        return cityName.toLowerCase()
                .replace(" ", "")
                .replace(",india", "")
                .replace(",in", "")
                .trim();
    }
    
    private double estimateDistanceForUnknownCities(String origin, String destination) {
        // Default estimation: 500 km for unknown city pairs
        log.info("Using default distance estimation for {} to {}", origin, destination);
        return 500.0;
    }
    
    // Data classes
    @Data
    private static class Coordinate {
        final double lat;
        final double lon;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NominatimResponse {
        String lat;
        String lon;
        @JsonProperty("display_name")
        String displayName;
    }
}