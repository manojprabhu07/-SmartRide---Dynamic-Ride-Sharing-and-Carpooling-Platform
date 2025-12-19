package com.ridesharing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ridesharing.entity.DriverDetail;
import com.ridesharing.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverDetailRepository extends JpaRepository<DriverDetail, Long> {
    
    Optional<DriverDetail> findByUser(User user);
    
    Optional<DriverDetail> findByUserId(Long userId);
    
    boolean existsByLicenseNumber(String licenseNumber);
    
    boolean existsByCarNumber(String carNumber);
    
    Optional<DriverDetail> findByLicenseNumber(String licenseNumber);
    
    Optional<DriverDetail> findByCarNumber(String carNumber);
    
    // Find all unverified drivers for admin verification
    List<DriverDetail> findByIsVerifiedFalse();
}