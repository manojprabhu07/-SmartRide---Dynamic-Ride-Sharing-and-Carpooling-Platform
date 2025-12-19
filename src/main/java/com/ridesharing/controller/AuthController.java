package com.ridesharing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ridesharing.dto.LoginDto;
import com.ridesharing.dto.OtpVerificationDto;
import com.ridesharing.dto.UserRegistrationDto;
import com.ridesharing.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    // Test endpoint for connectivity

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            return ResponseEntity.ok(authService.registerUser(registrationDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginDto loginDto) {
        try {
            System.out.println("Login attempt for phone: " + loginDto.getPhoneNumber());
            return ResponseEntity.ok(authService.authenticateUser(loginDto));
        } catch (Exception e) {
            System.out.println("Login failed for phone: " + loginDto.getPhoneNumber() + ", Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationDto otpDto) {
        try {
            return ResponseEntity.ok(authService.verifyOtp(otpDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String phoneNumber) {
        try {
            return ResponseEntity.ok(authService.resendOtp(phoneNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            return ResponseEntity.ok(authService.refreshToken(refreshToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        return ResponseEntity.ok("Backend connection successful!");
    }
}