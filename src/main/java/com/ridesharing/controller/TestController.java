package com.ridesharing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ridesharing.dto.ApiResponse;
import com.ridesharing.service.EmailService;
import com.ridesharing.service.UserService;
import com.ridesharing.entity.User;

@RestController
@RequestMapping("/api")
public class TestController {

    private final UserService userService;
    private final EmailService emailService;

    public TestController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        return ResponseEntity.ok("Backend connection successful! CORS is working.");
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> testPost() {
        return ResponseEntity.ok("POST request successful! CORS is working.");
    }
    
    @GetMapping("/check-user")
    public ResponseEntity<?> checkUser(@RequestParam String phoneNumber) {
        try {
            User user = userService.getUserByPhoneNumber(phoneNumber);
            return ResponseEntity.ok(java.util.Map.of(
                "exists", true,
                "phoneNumber", user.getPhoneNumber(),
                "firstName", user.getFirstName(),
                "isVerified", user.getIsVerified(),
                "isActive", user.getIsActive(),
                "role", user.getRole()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of(
                "exists", false,
                "phoneNumber", phoneNumber,
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse> testEmail(
            @RequestParam String to, 
            @RequestParam String subject, 
            @RequestParam String message) {
        try {
            emailService.sendSimpleEmail(to, subject, message);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Test email sent successfully to " + to,
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                "Failed to send email: " + e.getMessage(),
                null
            ));
        }
    }
}