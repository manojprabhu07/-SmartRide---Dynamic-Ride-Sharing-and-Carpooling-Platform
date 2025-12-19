package com.ridesharing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ridesharing.entity.Ride;
import com.ridesharing.entity.RideStatus;
import com.ridesharing.entity.User;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    // Find rides by driver
    List<Ride> findByDriverAndStatusOrderByDepartureDateDesc(User driver, RideStatus status);
    
    List<Ride> findByDriverOrderByDepartureDateDesc(User driver);

    // Search rides with filters
    @Query("SELECT r FROM Ride r WHERE " +
           "(:source IS NULL OR LOWER(r.source) LIKE LOWER(CONCAT('%', :source, '%'))) AND " +
           "(:destination IS NULL OR LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) AND " +
           "(:departureDate IS NULL OR DATE(r.departureDate) = DATE(:departureDate)) AND " +
           "(:minSeats IS NULL OR r.availableSeats >= :minSeats) AND " +
           "(:maxPrice IS NULL OR r.pricePerSeat <= :maxPrice) AND " +
           "(:vehicleType IS NULL OR LOWER(r.vehicleType) LIKE LOWER(CONCAT('%', :vehicleType, '%'))) AND " +
           "r.status = 'ACTIVE' AND r.departureDate > CURRENT_TIMESTAMP AND r.availableSeats > 0 " +
           "ORDER BY r.departureDate ASC")
    Page<Ride> searchAvailableRides(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("departureDate") LocalDateTime departureDate,
            @Param("minSeats") Integer minSeats,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("vehicleType") String vehicleType,
            Pageable pageable);

    // Find upcoming rides by driver
    @Query("SELECT r FROM Ride r WHERE r.driver = :driver AND r.departureDate > CURRENT_TIMESTAMP ORDER BY r.departureDate ASC")
    List<Ride> findUpcomingRidesByDriver(@Param("driver") User driver);

    // Find past rides by driver
    @Query("SELECT r FROM Ride r WHERE r.driver = :driver AND r.departureDate < CURRENT_TIMESTAMP ORDER BY r.departureDate DESC")
    List<Ride> findPastRidesByDriver(@Param("driver") User driver);

    // Find active rides with available seats
    @Query("SELECT r FROM Ride r WHERE r.status = 'ACTIVE' AND r.departureDate > CURRENT_TIMESTAMP AND r.availableSeats > 0 ORDER BY r.departureDate ASC")
    Page<Ride> findActiveRidesWithAvailableSeats(Pageable pageable);

    // Count rides by driver
    long countByDriver(User driver);

    // Find rides by source and destination
    List<Ride> findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCaseAndStatusAndDepartureDateGreaterThan(
            String source, String destination, RideStatus status, LocalDateTime currentTime);
}