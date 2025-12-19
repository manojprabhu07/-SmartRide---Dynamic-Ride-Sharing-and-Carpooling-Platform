package com.ridesharing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ridesharing.dto.ApiResponse;
import com.ridesharing.dto.DriverDetailDto;
import com.ridesharing.entity.DriverDetail;
import com.ridesharing.service.DriverDetailService;
import com.ridesharing.security.JwtTokenProvider;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverDetailService driverDetailService;
    private final JwtTokenProvider jwtTokenProvider;

    public DriverController(DriverDetailService driverDetailService, JwtTokenProvider jwtTokenProvider) {
        this.driverDetailService = driverDetailService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/details")
    public ResponseEntity<ApiResponse> addDriverDetails(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody DriverDetailDto driverDetailDto) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            DriverDetail driverDetail = driverDetailService.addDriverDetails(phoneNumber, driverDetailDto);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Driver details added successfully",
                driverDetail
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while adding driver details",
                null
            ));
        }
    }

    @PutMapping("/details")
    public ResponseEntity<ApiResponse> updateDriverDetails(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody DriverDetailDto driverDetailDto) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            DriverDetail driverDetail = driverDetailService.updateDriverDetails(phoneNumber, driverDetailDto);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Driver details updated successfully",
                driverDetail
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while updating driver details",
                null
            ));
        }
    }

    @GetMapping("/details")
    public ResponseEntity<ApiResponse> getDriverDetails(
            @RequestHeader("Authorization") String token) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            DriverDetail driverDetail = driverDetailService.getDriverDetails(phoneNumber);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Driver details retrieved successfully",
                driverDetail
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while retrieving driver details",
                null
            ));
        }
    }

    @DeleteMapping("/details")
    public ResponseEntity<ApiResponse> deleteDriverDetails(
            @RequestHeader("Authorization") String token) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            driverDetailService.deleteDriverDetails(phoneNumber);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Driver details deleted successfully",
                null
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while deleting driver details",
                null
            ));
        }
    }

    @GetMapping("/details/check")
    public ResponseEntity<ApiResponse> checkDriverDetails(
            @RequestHeader("Authorization") String token) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            boolean hasDetails = driverDetailService.hasDriverDetails(phoneNumber);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Driver details check completed",
                hasDetails
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while checking driver details",
                null
            ));
        }
    }

    @PutMapping("/verify/{driverDetailId}")
    public ResponseEntity<ApiResponse> verifyDriverDetails(
            @PathVariable Long driverDetailId,
            @RequestParam boolean verified) {
        try {
            DriverDetail driverDetail = driverDetailService.verifyDriverDetails(driverDetailId, verified);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                verified ? "Driver details verified successfully" : "Driver details marked as unverified",
                driverDetail
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while verifying driver details",
                null
            ));
        }
    }
}