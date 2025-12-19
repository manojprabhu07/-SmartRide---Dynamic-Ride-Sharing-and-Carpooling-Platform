package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverWithRatingDto {
    
    // User Information
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private LocalDateTime createdAt;
    
    // Driver Details
    private Long driverDetailId;
    private String licenseNumber;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleYear;
    private String vehicleColor;
    private String vehiclePlateNumber;
    private Boolean isVerified;
    private String verificationStatus;
    
    // Rating Information
    private Double averageRating;
    private Long totalRatings;
    private String ratingDisplay; // e.g., "4.5 (23 reviews)"
    
    // Star Distribution
    private Long fiveStarCount;
    private Long fourStarCount;
    private Long threeStarCount;
    private Long twoStarCount;
    private Long oneStarCount;
    
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return "Unknown";
    }
    
    public String getRatingDisplay() {
        if (averageRating != null && totalRatings != null && totalRatings > 0) {
            return String.format("%.1f (%d review%s)", 
                averageRating, 
                totalRatings, 
                totalRatings == 1 ? "" : "s");
        }
        return "No ratings yet";
    }
    
    public String getVerificationStatus() {
        if (isVerified != null) {
            return isVerified ? "Verified" : "Pending";
        }
        return "Unknown";
    }
}