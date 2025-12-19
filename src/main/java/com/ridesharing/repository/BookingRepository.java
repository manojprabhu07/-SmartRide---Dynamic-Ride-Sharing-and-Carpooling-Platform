package com.ridesharing.repository;

import com.ridesharing.entity.Booking;
import com.ridesharing.entity.BookingStatus;
import com.ridesharing.entity.Ride;
import com.ridesharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find bookings by passenger
    List<Booking> findByPassengerOrderByBookingDateDesc(User passenger);
    
    List<Booking> findByPassengerAndStatusOrderByBookingDateDesc(User passenger, BookingStatus status);

    // Find bookings by ride
    List<Booking> findByRideOrderByBookingDateAsc(Ride ride);
    
    List<Booking> findByRideOrderByBookingDateDesc(Ride ride);
    
    List<Booking> findByRideAndStatusOrderByBookingDateAsc(Ride ride, BookingStatus status);

    // Check if passenger already booked this ride
    Optional<Booking> findByRideAndPassengerAndStatus(Ride ride, User passenger, BookingStatus status);

    // Count confirmed bookings for a ride
    @Query("SELECT COALESCE(SUM(b.seatsBooked), 0) FROM Booking b WHERE b.ride = :ride AND b.status = :status")
    Integer countConfirmedSeatsByRide(@Param("ride") Ride ride, @Param("status") BookingStatus status);

    // Find bookings by ride and status
    @Query("SELECT b FROM Booking b WHERE b.ride.driver = :driver AND b.status = :status ORDER BY b.bookingDate DESC")
    List<Booking> findByDriverAndStatus(@Param("driver") User driver, @Param("status") BookingStatus status);

    // Find all bookings for rides by a specific driver
    @Query("SELECT b FROM Booking b WHERE b.ride.driver = :driver ORDER BY b.bookingDate DESC")
    List<Booking> findByDriver(@Param("driver") User driver);

    // Find upcoming bookings for passenger
    @Query("SELECT b FROM Booking b WHERE b.passenger = :passenger AND b.ride.departureDate > CURRENT_TIMESTAMP AND b.status = :status ORDER BY b.ride.departureDate ASC")
    List<Booking> findUpcomingBookingsByPassenger(@Param("passenger") User passenger, @Param("status") BookingStatus status);

    // Find past bookings for passenger
    @Query("SELECT b FROM Booking b WHERE b.passenger = :passenger AND b.ride.departureDate < CURRENT_TIMESTAMP ORDER BY b.ride.departureDate DESC")
    List<Booking> findPastBookingsByPassenger(@Param("passenger") User passenger);

    // Count total bookings by passenger
    long countByPassenger(User passenger);

    // Check if passenger has any active bookings for a specific ride
    boolean existsByRideAndPassengerAndStatus(Ride ride, User passenger, BookingStatus status);
}