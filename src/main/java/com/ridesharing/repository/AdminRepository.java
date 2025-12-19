package com.ridesharing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ridesharing.entity.Admin;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);
    
    Optional<Admin> findByEmailAndIsActiveTrue(String email);
    
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE Admin a SET a.lastLogin = :loginTime WHERE a.id = :adminId")
    void updateLastLogin(@Param("adminId") Long adminId, @Param("loginTime") LocalDateTime loginTime);
}