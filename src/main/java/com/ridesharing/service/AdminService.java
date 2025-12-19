package com.ridesharing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.dto.AdminLoginDto;
import com.ridesharing.dto.AdminProfileDto;
import com.ridesharing.dto.ApiResponse;
import com.ridesharing.dto.DriverWithRatingDto;
import com.ridesharing.entity.Admin;
import com.ridesharing.entity.DriverDetail;
import com.ridesharing.entity.User;
import com.ridesharing.exception.UserNotFoundException;
import com.ridesharing.repository.AdminRepository;
import com.ridesharing.repository.DriverDetailRepository;
import com.ridesharing.repository.RatingRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final DriverDetailRepository driverDetailRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public ApiResponse authenticateAdmin(AdminLoginDto loginDto) {
        try {
            // Find admin by email
            Admin admin = adminRepository.findByEmailAndIsActiveTrue(loginDto.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("Invalid admin credentials"));

            // Verify password (plain text comparison)
            if (!loginDto.getPassword().equals(admin.getPassword())) {
                throw new UserNotFoundException("Invalid admin credentials");
            }

            // Update last login
            adminRepository.updateLastLogin(admin.getId(), LocalDateTime.now());

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAdminToken(admin.getEmail(), admin.getId());
            String refreshToken = jwtTokenProvider.generateAdminRefreshToken(admin.getEmail());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtTokenProvider.getJwtExpirationInMs() / 1000);
            response.put("admin", convertToDto(admin));

            log.info("Admin login successful for email: {}", admin.getEmail());
            return new ApiResponse("SUCCESS", "Admin login successful", response);

        } catch (Exception e) {
            log.error("Admin login failed: {}", e.getMessage());
            throw new UserNotFoundException("Invalid admin credentials");
        }
    }

    public ApiResponse getAdminProfile(String email) {
        Admin admin = adminRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UserNotFoundException("Admin not found"));

        return new ApiResponse("SUCCESS", "Admin profile retrieved successfully", convertToDto(admin));
    }

    @Transactional(readOnly = true)
    public ApiResponse getAllDriversWithRatings() {
        try {
            log.info("Fetching all drivers with their ratings for admin panel");
            
            // Get all driver details
            List<DriverDetail> driverDetails = driverDetailRepository.findAll();
            
            // Convert to DTO with rating information
            List<DriverWithRatingDto> driversWithRatings = driverDetails.stream()
                    .map(this::convertToDriverWithRatingDto)
                    .collect(Collectors.toList());
            
            log.info("Successfully retrieved {} drivers with rating information", driversWithRatings.size());
            return new ApiResponse("SUCCESS", "Drivers with ratings retrieved successfully", driversWithRatings);
            
        } catch (Exception e) {
            log.error("Error fetching drivers with ratings: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch drivers with ratings: " + e.getMessage());
        }
    }

    private DriverWithRatingDto convertToDriverWithRatingDto(DriverDetail driverDetail) {
        User user = driverDetail.getUser();
        
        // Get rating information
        Double averageRating = ratingRepository.getAverageRatingForDriver(user);
        Long totalRatings = ratingRepository.getRatingCountForDriver(user);
        
        // Get star distribution
        Long fiveStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(user, 5).size();
        Long fourStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(user, 4).size();
        Long threeStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(user, 3).size();
        Long twoStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(user, 2).size();
        Long oneStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(user, 1).size();
        
        return DriverWithRatingDto.builder()
                // User Information
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                
                // Driver Details
                .driverDetailId(driverDetail.getId())
                .licenseNumber(driverDetail.getLicenseNumber())
                .vehicleMake(driverDetail.getCarModel()) // carModel maps to vehicleMake
                .vehicleModel(driverDetail.getCarModel())
                .vehicleYear(driverDetail.getCarYear() != null ? driverDetail.getCarYear().toString() : null)
                .vehicleColor(driverDetail.getCarColor())
                .vehiclePlateNumber(driverDetail.getCarNumber()) // carNumber maps to vehiclePlateNumber
                .isVerified(driverDetail.getIsVerified())
                
                // Rating Information
                .averageRating(averageRating != null ? averageRating : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0L)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .build();
    }

    private AdminProfileDto convertToDto(Admin admin) {
        AdminProfileDto dto = new AdminProfileDto();
        dto.setId(admin.getId());
        dto.setEmail(admin.getEmail());
        dto.setFirstName(admin.getFirstName());
        dto.setLastName(admin.getLastName());
        dto.setIsActive(admin.getIsActive());
        dto.setCreatedAt(admin.getCreatedAt());
        dto.setLastLogin(admin.getLastLogin());
        return dto;
    }
}