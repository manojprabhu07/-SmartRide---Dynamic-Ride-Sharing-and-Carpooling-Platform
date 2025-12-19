package com.ridesharing.repository;

import com.ridesharing.entity.Rating;
import com.ridesharing.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Find all ratings for a specific driver
    List<Rating> findByDriverOrderByCreatedAtDesc(User driver);

    // Find all ratings given by a specific passenger
    List<Rating> findByPassengerOrderByCreatedAtDesc(User passenger);

    // Find rating for a specific booking
    Optional<Rating> findByBookingId(Long bookingId);

    // Check if passenger has already rated a driver for a specific booking
    boolean existsByPassengerAndDriverAndBookingId(User passenger, User driver, Long bookingId);

    // Get average rating for a driver
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.driver = :driver")
    Double getAverageRatingForDriver(@Param("driver") User driver);

    // Get rating count for a driver
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.driver = :driver")
    Long getRatingCountForDriver(@Param("driver") User driver);

    // Get all ratings for a driver with pagination
    Page<Rating> findByDriverOrderByCreatedAtDesc(User driver, Pageable pageable);

    // Find ratings by rating value for a driver
    List<Rating> findByDriverAndRatingOrderByCreatedAtDesc(User driver, Integer rating);

    // Get recent ratings for a driver (last 10)
    @Query("SELECT r FROM Rating r WHERE r.driver = :driver ORDER BY r.createdAt DESC LIMIT 10")
    List<Rating> getRecentRatingsForDriver(@Param("driver") User driver);

    // Get ratings with comments for a driver
    @Query("SELECT r FROM Rating r WHERE r.driver = :driver AND r.comment IS NOT NULL AND r.comment != '' ORDER BY r.createdAt DESC")
    List<Rating> getRatingsWithCommentsForDriver(@Param("driver") User driver);

    // Get all ratings (admin function)
    Page<Rating> findAllByOrderByCreatedAtDesc(Pageable pageable);
}