package com.ridesharing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.entity.OtpVerification;
import com.ridesharing.repository.OtpVerificationRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final TwilioService twilioService;

    // OTP settings
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_LENGTH = 6;

    public OtpService(OtpVerificationRepository otpRepository, TwilioService twilioService) {
        this.otpRepository = otpRepository;
        this.twilioService = twilioService;
    }

    public String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }

    public void generateAndSendOtp(String phoneNumber) {
        // Delete any existing OTPs for this phone number
        otpRepository.deleteByPhoneNumber(phoneNumber);

        // Generate new OTP
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Save OTP to database
        OtpVerification otpVerification = new OtpVerification(phoneNumber, otp, expiresAt);
        otpRepository.save(otpVerification);

        // For development - always log OTP to console
        System.out.println("==============================================");
        System.out.println("🔐 OTP FOR DEVELOPMENT");
        System.out.println("📱 Phone: " + phoneNumber);
        System.out.println("🔢 OTP: " + otp);
        System.out.println("⏰ Expires: " + expiresAt);
        System.out.println("==============================================");

        // Try to send SMS, but don't fail if it doesn't work
        try {
            twilioService.sendOtp(phoneNumber, otp);
            System.out.println("✅ SMS sent successfully via Twilio");
        } catch (Exception e) {
            System.err.println("❌ Twilio SMS failed (using console OTP): " + e.getMessage());
            System.out.println("💡 Use the OTP printed above for testing");
        }
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        Optional<OtpVerification> otpVerificationOpt = otpRepository.findByPhoneNumberAndOtpAndIsUsedFalse(phoneNumber, otp);

        if (otpVerificationOpt.isPresent()) {
            OtpVerification otpVerification = otpVerificationOpt.get();
            
            if (otpVerification.isValid()) {
                // Mark OTP as used
                otpVerification.markAsUsed();
                otpRepository.save(otpVerification);
                return true;
            }
        }

        return false;
    }

    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    public boolean canSendOtp(String phoneNumber) {
        // Limit OTP generation to prevent abuse
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long otpCount = otpRepository.countByPhoneNumberAndCreatedAtAfter(phoneNumber, oneHourAgo);
        
        // Allow maximum 5 OTPs per hour
        return otpCount < 5;
    }
}