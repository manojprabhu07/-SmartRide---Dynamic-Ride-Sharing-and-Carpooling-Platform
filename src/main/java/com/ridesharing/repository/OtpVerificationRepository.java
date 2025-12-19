package com.ridesharing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ridesharing.entity.OtpVerification;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    
    Optional<OtpVerification> findByPhoneNumberAndIsUsedFalse(String phoneNumber);
    
    Optional<OtpVerification> findByPhoneNumberAndOtpAndIsUsedFalse(String phoneNumber, String otp);
    
    void deleteByPhoneNumber(String phoneNumber);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, LocalDateTime dateTime);
}