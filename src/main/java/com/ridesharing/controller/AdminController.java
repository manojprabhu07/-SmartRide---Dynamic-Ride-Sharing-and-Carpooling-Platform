package com.ridesharing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ridesharing.dto.AdminLoginDto;
import com.ridesharing.dto.ApiResponse;
import com.ridesharing.entity.DriverDetail;
import com.ridesharing.entity.User;
import com.ridesharing.security.JwtTokenProvider;
import com.ridesharing.service.AdminService;
import com.ridesharing.service.DriverDetailService;
import com.ridesharing.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DriverDetailService driverDetailService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> adminLogin(@Valid @RequestBody AdminLoginDto loginDto) {
        try {
            ApiResponse response = adminService.authenticateAdmin(loginDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getAdminProfile(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            String email = jwtTokenProvider.getSubjectFromJWT(token);
            
            ApiResponse response = adminService.getAdminProfile(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse> getAllDriverDetails() {
        try {
            ApiResponse response = driverDetailService.getAllDriverDetails();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/drivers-with-ratings")
    public ResponseEntity<ApiResponse> getAllDriversWithRatings() {
        try {
            ApiResponse response = adminService.getAllDriversWithRatings();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/drivers/pending")
    public ResponseEntity<ApiResponse> getPendingDriverDetails() {
        try {
            ApiResponse response = driverDetailService.getPendingDriverDetails();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            ApiResponse response = new ApiResponse("SUCCESS", "Users retrieved successfully", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            ApiResponse response = new ApiResponse("SUCCESS", "User retrieved successfully", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            ApiResponse response = new ApiResponse("SUCCESS", "User deleted successfully", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @PutMapping("/drivers/{driverDetailId}/verify")
    public ResponseEntity<ApiResponse> verifyDriver(@PathVariable Long driverDetailId) {
        try {
            DriverDetail verifiedDriver = driverDetailService.verifyDriverDetails(driverDetailId, true);
            ApiResponse response = new ApiResponse("SUCCESS", "Driver verified successfully", verifiedDriver);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    @PutMapping("/drivers/{driverDetailId}/reject")
    public ResponseEntity<ApiResponse> rejectDriver(@PathVariable Long driverDetailId) {
        try {
            DriverDetail rejectedDriver = driverDetailService.verifyDriverDetails(driverDetailId, false);
            ApiResponse response = new ApiResponse("SUCCESS", "Driver rejected successfully", rejectedDriver);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("No valid JWT token found");
    }
}