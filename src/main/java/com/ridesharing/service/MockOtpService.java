package com.ridesharing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ridesharing.entity.OtpVerification;
import com.ridesharing.repository.OtpVerificationRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MockOtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    
    @Value("${app.mock-otp:false}")
    private boolean mockOtpEnabled;

    public void generateAndSendOtp(String phoneNumber) {
        // Invalidate any existing OTP
        Optional<OtpVerification> existingOtp = otpVerificationRepository.findByPhoneNumberAndIsUsedFalse(phoneNumber);
        existingOtp.ifPresent(otp -> {
            otp.setIsUsed(true);
            otpVerificationRepository.save(otp);
        });

        // Generate new OTP
        String otp = generateOtp();

        // Save OTP to database
        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setPhoneNumber(phoneNumber);
        otpVerification.setOtp(otp);
        otpVerification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpVerification.setIsUsed(false);
        otpVerificationRepository.save(otpVerification);

        if (mockOtpEnabled) {
            // Mock mode - just log the OTP
            System.out.println("==============================================");
            System.out.println("🔐 MOCK OTP FOR DEVELOPMENT");
            System.out.println("📱 Phone: " + phoneNumber);
            System.out.println("🔢 OTP: " + otp);
            System.out.println("⏰ Expires: " + otpVerification.getExpiresAt());
            System.out.println("==============================================");
        } else {
            // Try to send real SMS, but handle trial account gracefully
            try {
                // Your existing Twilio logic here
                sendRealSms(phoneNumber, otp);
            } catch (Exception e) {
                // If Twilio fails, log OTP for development
                System.err.println("Twilio SMS failed: " + e.getMessage());
                System.out.println("==============================================");
                System.out.println("🔐 FALLBACK OTP (Twilio Failed)");
                System.out.println("📱 Phone: " + phoneNumber);
                System.out.println("🔢 OTP: " + otp);
                System.out.println("==============================================");
            }
        }
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        Optional<OtpVerification> otpVerificationOpt = 
            otpVerificationRepository.findByPhoneNumberAndOtpAndIsUsedFalse(phoneNumber, otp);

        if (otpVerificationOpt.isPresent()) {
            OtpVerification otpVerification = otpVerificationOpt.get();
            
            if (otpVerification.getExpiresAt().isAfter(LocalDateTime.now())) {
                otpVerification.setIsUsed(true);
                otpVerification.setVerifiedAt(LocalDateTime.now());
                otpVerificationRepository.save(otpVerification);
                return true;
            }
        }
        return false;
    }

    private void sendRealSms(String phoneNumber, String otp) {
        // Your existing Twilio SMS logic
        // This will be called when not in mock mode
        throw new RuntimeException("Twilio trial account cannot send to unverified numbers");
    }

    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }
}