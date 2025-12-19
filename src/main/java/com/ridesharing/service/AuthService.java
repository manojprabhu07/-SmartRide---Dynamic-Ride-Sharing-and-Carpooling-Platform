package com.ridesharing.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.dto.LoginDto;
import com.ridesharing.dto.OtpVerificationDto;
import com.ridesharing.dto.UserRegistrationDto;
import com.ridesharing.entity.User;
import com.ridesharing.entity.UserRole;
import com.ridesharing.exception.InvalidOtpException;
import com.ridesharing.exception.UserAlreadyExistsException;
import com.ridesharing.security.JwtTokenProvider;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class AuthService {

    private final UserService userService;
    private final OtpService otpService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserService userService, OtpService otpService, 
                      JwtTokenProvider tokenProvider, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.otpService = otpService;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public Map<String, Object> registerUser(UserRegistrationDto registrationDto) {
        // Check if user already exists
        if (userService.existsByPhoneNumber(registrationDto.getPhoneNumber())) {
            throw new UserAlreadyExistsException("phoneNumber", registrationDto.getPhoneNumber());
        }
        if (userService.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("email", registrationDto.getEmail());
        }

        // Create user
        UserRole role;
        if (registrationDto.getRole() == null || registrationDto.getRole().isEmpty()) {
            // Default to USER if no role specified
            role = UserRole.USER;
        } else {
            role = UserRole.valueOf(registrationDto.getRole().toUpperCase());
        }
        User user = userService.createUser(
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getPhoneNumber(),
                registrationDto.getEmail(),
                registrationDto.getPassword(),
                role
        );

        // Send OTP
        otpService.generateAndSendOtp(user.getPhoneNumber());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully. Please verify your phone number with OTP.");
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("userId", user.getId());

        return response;
    }

    public Map<String, Object> authenticateUser(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getPhoneNumber(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userService.getUserByPhoneNumber(loginDto.getPhoneNumber());
        
        String jwt = tokenProvider.generateToken(user.getPhoneNumber(), user.getId(), user.getRole().toString());
        String refreshToken = tokenProvider.generateRefreshToken(user.getPhoneNumber());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", jwt);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("user", Map.of(
                "id", user.getId(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "phoneNumber", user.getPhoneNumber(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "isVerified", user.getIsVerified()
        ));

        return response;
    }

    public Map<String, Object> verifyOtp(OtpVerificationDto otpDto) {
        if (!otpService.verifyOtp(otpDto.getPhoneNumber(), otpDto.getOtp())) {
            throw new InvalidOtpException();
        }

        // Mark user as verified
        userService.verifyUser(otpDto.getPhoneNumber());

        // Generate tokens for the verified user
        User user = userService.getUserByPhoneNumber(otpDto.getPhoneNumber());
        String jwt = tokenProvider.generateToken(user.getPhoneNumber(), user.getId(), user.getRole().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Phone number verified successfully");
        response.put("accessToken", jwt);
        response.put("tokenType", "Bearer");
        response.put("user", Map.of(
                "id", user.getId(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "phoneNumber", user.getPhoneNumber(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "isVerified", user.getIsVerified()
        ));

        return response;
    }

    public Map<String, Object> resendOtp(String phoneNumber) {
        // Check if user exists and generate new OTP
        userService.getUserByPhoneNumber(phoneNumber);
        
        // Generate and send new OTP
        otpService.generateAndSendOtp(phoneNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP sent successfully");
        response.put("phoneNumber", phoneNumber);

        return response;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (tokenProvider.validateToken(refreshToken)) {
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            User user = userService.getUserByPhoneNumber(username);
            String newToken = tokenProvider.generateToken(user.getPhoneNumber(), user.getId(), user.getRole().toString());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newToken);
            response.put("tokenType", "Bearer");

            return response;
        } else {
            throw new InvalidOtpException("Invalid refresh token");
        }
    }
}