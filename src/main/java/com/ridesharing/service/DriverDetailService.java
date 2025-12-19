package com.ridesharing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.dto.ApiResponse;
import com.ridesharing.dto.DriverDetailDto;
import com.ridesharing.entity.DriverDetail;
import com.ridesharing.entity.User;
import com.ridesharing.entity.UserRole;
import com.ridesharing.repository.DriverDetailRepository;

import java.util.List;

@Service
@Transactional
public class DriverDetailService {

    private final DriverDetailRepository driverDetailRepository;
    private final UserService userService;

    public DriverDetailService(DriverDetailRepository driverDetailRepository, UserService userService) {
        this.driverDetailRepository = driverDetailRepository;
        this.userService = userService;
    }

    public DriverDetail addDriverDetails(String phoneNumber, DriverDetailDto driverDetailDto) {
        User user = userService.getUserByPhoneNumber(phoneNumber);
        
        // Check if user is a driver
        if (!user.getRole().equals(UserRole.DRIVER)) {
            throw new RuntimeException("Only drivers can add driver details");
        }

        // Check if driver details already exist
        if (driverDetailRepository.findByUser(user).isPresent()) {
            throw new RuntimeException("Driver details already exist for this user");
        }

        // Check if license number already exists
        if (driverDetailRepository.existsByLicenseNumber(driverDetailDto.getLicenseNumber())) {
            throw new RuntimeException("License number already exists");
        }

        // Check if car number already exists
        if (driverDetailRepository.existsByCarNumber(driverDetailDto.getCarNumber())) {
            throw new RuntimeException("Car number already exists");
        }

        // Create new driver details
        DriverDetail driverDetail = new DriverDetail();
        driverDetail.setUser(user);
        driverDetail.setLicenseNumber(driverDetailDto.getLicenseNumber());
        driverDetail.setLicenseExpiry(driverDetailDto.getLicenseExpiryDate().atStartOfDay());
        driverDetail.setCarNumber(driverDetailDto.getCarNumber());
        driverDetail.setCarModel(driverDetailDto.getCarModel());
        driverDetail.setCarColor(driverDetailDto.getCarColor());
        driverDetail.setCarYear(driverDetailDto.getCarYear());
        driverDetail.setInsuranceNumber(driverDetailDto.getInsuranceNumber());
        driverDetail.setInsuranceExpiry(driverDetailDto.getInsuranceExpiryDate().atStartOfDay());

        return driverDetailRepository.save(driverDetail);
    }

    public DriverDetail updateDriverDetails(String phoneNumber, DriverDetailDto driverDetailDto) {
        User user = userService.getUserByPhoneNumber(phoneNumber);
        
        DriverDetail existingDetail = driverDetailRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver details not found. Please add details first."));

        // Check if license number is being changed and if new one already exists
        if (!existingDetail.getLicenseNumber().equals(driverDetailDto.getLicenseNumber()) &&
            driverDetailRepository.existsByLicenseNumber(driverDetailDto.getLicenseNumber())) {
            throw new RuntimeException("License number already exists");
        }

        // Check if car number is being changed and if new one already exists
        if (!existingDetail.getCarNumber().equals(driverDetailDto.getCarNumber()) &&
            driverDetailRepository.existsByCarNumber(driverDetailDto.getCarNumber())) {
            throw new RuntimeException("Car number already exists");
        }

        // Update details
        existingDetail.setLicenseNumber(driverDetailDto.getLicenseNumber());
        existingDetail.setLicenseExpiry(driverDetailDto.getLicenseExpiryDate().atStartOfDay());
        existingDetail.setCarNumber(driverDetailDto.getCarNumber());
        existingDetail.setCarModel(driverDetailDto.getCarModel());
        existingDetail.setCarColor(driverDetailDto.getCarColor());
        existingDetail.setCarYear(driverDetailDto.getCarYear());
        existingDetail.setInsuranceNumber(driverDetailDto.getInsuranceNumber());
        existingDetail.setInsuranceExpiry(driverDetailDto.getInsuranceExpiryDate().atStartOfDay());

        return driverDetailRepository.save(existingDetail);
    }

    public DriverDetail getDriverDetails(String phoneNumber) {
        User user = userService.getUserByPhoneNumber(phoneNumber);
        return driverDetailRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver details not found"));
    }

    public void deleteDriverDetails(String phoneNumber) {
        User user = userService.getUserByPhoneNumber(phoneNumber);
        DriverDetail driverDetail = driverDetailRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver details not found"));
        
        driverDetailRepository.delete(driverDetail);
    }

    public DriverDetail verifyDriverDetails(Long driverDetailId, boolean verified) {
        DriverDetail driverDetail = driverDetailRepository.findById(driverDetailId)
                .orElseThrow(() -> new RuntimeException("Driver details not found"));
        
        driverDetail.setIsVerified(verified);
        return driverDetailRepository.save(driverDetail);
    }

    public boolean hasDriverDetails(String phoneNumber) {
        User user = userService.getUserByPhoneNumber(phoneNumber);
        return driverDetailRepository.findByUser(user).isPresent();
    }

    public ApiResponse getAllDriverDetails() {
        try {
            List<DriverDetail> allDrivers = driverDetailRepository.findAll();
            return new ApiResponse("SUCCESS", "Driver details retrieved successfully", allDrivers);
        } catch (Exception e) {
            return new ApiResponse("ERROR", "Failed to retrieve driver details: " + e.getMessage(), null);
        }
    }

    public ApiResponse getPendingDriverDetails() {
        try {
            List<DriverDetail> pendingDrivers = driverDetailRepository.findByIsVerifiedFalse();
            return new ApiResponse("SUCCESS", "Pending driver details retrieved successfully", pendingDrivers);
        } catch (Exception e) {
            return new ApiResponse("ERROR", "Failed to retrieve pending driver details: " + e.getMessage(), null);
        }
    }
}