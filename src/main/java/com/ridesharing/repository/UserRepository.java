package com.ridesharing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ridesharing.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByPhoneNumberAndIsActiveTrue(String phoneNumber);
    
    Optional<User> findByEmailAndIsActiveTrue(String email);
    
    List<User> findByIsActiveTrue();
}