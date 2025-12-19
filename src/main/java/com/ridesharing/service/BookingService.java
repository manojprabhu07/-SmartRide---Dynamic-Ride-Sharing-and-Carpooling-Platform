package com.ridesharing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.dto.BookingDto;
import com.ridesharing.dto.BookingResponseDto;
import com.ridesharing.entity.Booking;
import com.ridesharing.entity.BookingStatus;
import com.ridesharing.entity.Ride;
import com.ridesharing.entity.RideStatus;
import com.ridesharing.entity.User;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.RideRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final RideReminderService reminderService;

    public BookingResponseDto bookRide(String phoneNumber, BookingDto bookingDto) {
        User passenger = userService.getUserByPhoneNumber(phoneNumber);
        
        // Get the ride
        Ride ride = rideRepository.findById(bookingDto.getRideId())
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        // Validate booking
        validateBooking(passenger, ride, bookingDto.getSeatsBooked());

        // Calculate total amount
        BigDecimal totalAmount = ride.getPricePerSeat()
                .multiply(BigDecimal.valueOf(bookingDto.getSeatsBooked()));

        // Create booking
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeatsBooked(bookingDto.getSeatsBooked());
        booking.setTotalAmount(totalAmount);
        booking.setPassengerName(bookingDto.getPassengerName());
        booking.setPassengerPhone(bookingDto.getPassengerPhone());
        booking.setPickupPoint(bookingDto.getPickupPoint());
        booking.setStatus(BookingStatus.PENDING); // Default status is PENDING

        // Update ride availability
        ride.setAvailableSeats(ride.getAvailableSeats() - bookingDto.getSeatsBooked());
        
        // Update ride status if fully booked
        if (ride.getAvailableSeats() == 0) {
            ride.setStatus(RideStatus.FULL);
        }

        rideRepository.save(ride);
        Booking savedBooking = bookingRepository.save(booking);
        
        return convertToResponseDto(savedBooking);
    }

    public List<BookingResponseDto> getPassengerBookings(String phoneNumber) {
        User passenger = userService.getUserByPhoneNumber(phoneNumber);
        List<Booking> bookings = bookingRepository.findByPassengerOrderByBookingDateDesc(passenger);
        
        return bookings.stream()
                      .map(this::convertToResponseDto)
                      .collect(Collectors.toList());
    }

    public List<BookingResponseDto> getDriverBookings(String phoneNumber) {
        User driver = userService.getUserByPhoneNumber(phoneNumber);
        List<Booking> bookings = bookingRepository.findByDriver(driver);
        
        return bookings.stream()
                      .map(this::convertToResponseDto)
                      .collect(Collectors.toList());
    }

    public List<BookingResponseDto> getUpcomingBookings(String phoneNumber) {
        User passenger = userService.getUserByPhoneNumber(phoneNumber);
        List<Booking> bookings = bookingRepository.findUpcomingBookingsByPassenger(passenger, BookingStatus.CONFIRMED);
        
        return bookings.stream()
                      .map(this::convertToResponseDto)
                      .collect(Collectors.toList());
    }

    public BookingResponseDto cancelBooking(String phoneNumber, Long bookingId) {
        User user = userService.getUserByPhoneNumber(phoneNumber);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Check if user owns this booking
        if (!booking.getPassenger().getId().equals(user.getId())) {
            throw new RuntimeException("You can only cancel your own bookings");
        }

        // Check if ride departure is not too close (e.g., at least 2 hours before)
        if (booking.getRide().getDepartureDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new RuntimeException("Cannot cancel booking less than 2 hours before departure");
        }

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);

        // Restore ride availability
        Ride ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        
        // Update ride status if it was full
        if (ride.getStatus() == RideStatus.FULL) {
            ride.setStatus(RideStatus.ACTIVE);
        }

        rideRepository.save(ride);
        Booking updatedBooking = bookingRepository.save(booking);
        
        // Cancel any scheduled reminders for this booking
        try {
            reminderService.cancelRemindersForBooking(bookingId);
        } catch (Exception e) {
            // Log error but don't fail the booking cancellation
            System.err.println("Failed to cancel ride reminders: " + e.getMessage());
        }
        
        return convertToResponseDto(updatedBooking);
    }

    public BookingResponseDto getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return convertToResponseDto(booking);
    }

    public List<BookingResponseDto> getConfirmedRideBookings(String phoneNumber, Long rideId) {
        User driver = userService.getUserByPhoneNumber(phoneNumber);
        
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        // Check if user owns this ride
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("You can only view bookings for your own rides");
        }

        List<Booking> bookings = bookingRepository.findByRideAndStatusOrderByBookingDateAsc(
            ride, BookingStatus.CONFIRMED);
        
        return bookings.stream()
                      .map(this::convertToResponseDto)
                      .collect(Collectors.toList());
    }

    // Driver booking management methods
    public BookingResponseDto confirmBookingByDriver(String driverPhoneNumber, Long rideId, Long bookingId) {
        User driver = userService.getUserByPhoneNumber(driverPhoneNumber);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Verify the booking belongs to the driver's ride
        if (!booking.getRide().getId().equals(rideId) || 
            !booking.getRide().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("You can only manage bookings for your own rides");
        }
        
        // Check if booking is in PENDING status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be confirmed");
        }
        
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        
        Booking savedBooking = bookingRepository.save(booking);
        
        // Send confirmation email to passenger
        try {
            emailService.sendBookingConfirmationEmail(
                booking.getPassenger(), 
                savedBooking, 
                booking.getRide(), 
                driver
            );
        } catch (Exception e) {
            // Log error but don't fail the booking confirmation
            System.err.println("Failed to send confirmation email: " + e.getMessage());
        }
        
        // Schedule ride reminders for the confirmed booking
        try {
            reminderService.scheduleRemindersForBooking(savedBooking);
        } catch (Exception e) {
            // Log error but don't fail the booking confirmation
            System.err.println("Failed to schedule ride reminders: " + e.getMessage());
        }
        
        return convertToResponseDto(savedBooking);
    }

    public BookingResponseDto cancelBookingByDriver(String driverPhoneNumber, Long rideId, Long bookingId) {
        User driver = userService.getUserByPhoneNumber(driverPhoneNumber);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Verify the booking belongs to the driver's ride
        if (!booking.getRide().getId().equals(rideId) || 
            !booking.getRide().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("You can only manage bookings for your own rides");
        }
        
        // Check if booking can be cancelled
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Completed bookings cannot be cancelled");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        
        // Restore available seats
        Ride ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        
        // Update ride status if it was FULL
        if (ride.getStatus() == RideStatus.FULL) {
            ride.setStatus(RideStatus.ACTIVE);
        }
        
        rideRepository.save(ride);
        Booking savedBooking = bookingRepository.save(booking);
        
        // Send cancellation email to passenger
        try {
            emailService.sendBookingCancellationEmail(
                booking.getPassenger(), 
                savedBooking, 
                booking.getRide(), 
                driver
            );
        } catch (Exception e) {
            // Log error but don't fail the booking cancellation
            System.err.println("Failed to send cancellation email: " + e.getMessage());
        }
        
        // Cancel any scheduled reminders for this booking
        try {
            reminderService.cancelRemindersForBooking(bookingId);
        } catch (Exception e) {
            // Log error but don't fail the booking cancellation
            System.err.println("Failed to cancel ride reminders: " + e.getMessage());
        }
        
        return convertToResponseDto(savedBooking);
    }

    public List<BookingResponseDto> getRideBookings(String driverPhoneNumber, Long rideId) {
        User driver = userService.getUserByPhoneNumber(driverPhoneNumber);
        
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        // Verify the ride belongs to the driver
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("You can only view bookings for your own rides");
        }
        
        List<Booking> bookings = bookingRepository.findByRideOrderByBookingDateDesc(ride);
        
        return bookings.stream()
                      .map(this::convertToResponseDto)
                      .collect(Collectors.toList());
    }

    private void validateBooking(User passenger, Ride ride, int seatsRequested) {
        // Check if ride is active
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new RuntimeException("This ride is not available for booking");
        }

        // Check if ride departure is in the future
        if (ride.getDepartureDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot book past rides");
        }

        // Check if enough seats are available
        if (ride.getAvailableSeats() < seatsRequested) {
            throw new RuntimeException("Not enough seats available. Only " + 
                ride.getAvailableSeats() + " seats remaining");
        }

        // Check if passenger is not the driver
        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new RuntimeException("Cannot book your own ride");
        }

        // Check if passenger hasn't already booked this ride
        if (bookingRepository.existsByRideAndPassengerAndStatus(ride, passenger, BookingStatus.CONFIRMED)) {
            throw new RuntimeException("You have already booked this ride");
        }
    }

    private BookingResponseDto convertToResponseDto(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(booking.getId());
        dto.setRideId(booking.getRide().getId());
        dto.setSource(booking.getRide().getSource());
        dto.setDestination(booking.getRide().getDestination());
        dto.setDepartureDate(booking.getRide().getDepartureDate());
        dto.setDriverId(booking.getRide().getDriver().getId());
        dto.setDriverName(booking.getRide().getDriver().getFirstName() + " " + 
                         booking.getRide().getDriver().getLastName());
        dto.setDriverPhone(booking.getRide().getDriver().getPhoneNumber());
        dto.setSeatsBooked(booking.getSeatsBooked());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setPassengerName(booking.getPassengerName());
        dto.setPassengerPhone(booking.getPassengerPhone());
        dto.setPickupPoint(booking.getPickupPoint());
        dto.setStatus(booking.getStatus());
        dto.setBookingDate(booking.getBookingDate());
        dto.setUpdatedAt(booking.getUpdatedAt());
        
        // Vehicle details
        dto.setVehicleModel(booking.getRide().getVehicleModel());
        dto.setVehicleColor(booking.getRide().getVehicleColor());
        dto.setVehicleNumber(booking.getRide().getVehicleNumber());
        dto.setVehicleMake(booking.getRide().getVehicleType());
        dto.setPricePerSeat(booking.getRide().getPricePerSeat());
        
        return dto;
    }

    public BookingResponseDto createBooking(String phoneNumber, Long rideId, Integer seatsToBook) {
        User passenger = userService.getUserByPhoneNumber(phoneNumber);
        
        // Get the ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        // Validate booking
        validateBooking(passenger, ride, seatsToBook);

        // Calculate total amount
        BigDecimal totalAmount = ride.getPricePerSeat()
                .multiply(BigDecimal.valueOf(seatsToBook));

        // Create booking
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeatsBooked(seatsToBook);
        booking.setTotalAmount(totalAmount);
        booking.setPassengerName(passenger.getFirstName() + " " + passenger.getLastName());
        booking.setPassengerPhone(passenger.getPhoneNumber());
        booking.setPickupPoint(ride.getSource()); // Default pickup point
        booking.setStatus(BookingStatus.PENDING); // Default status is PENDING

        // Update ride availability
        ride.setAvailableSeats(ride.getAvailableSeats() - seatsToBook);
        
        // Update ride status if fully booked
        if (ride.getAvailableSeats() == 0) {
            ride.setStatus(RideStatus.FULL);
        }

        rideRepository.save(ride);
        Booking savedBooking = bookingRepository.save(booking);
        
        return convertToResponseDto(savedBooking);
    }
}