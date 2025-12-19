package com.ridesharing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.dto.DistanceResponseDto;
import com.ridesharing.dto.RidePostDto;
import com.ridesharing.dto.RideResponseDto;
import com.ridesharing.dto.RideSearchDto;
import com.ridesharing.entity.Booking;
import com.ridesharing.entity.BookingStatus;
import com.ridesharing.entity.DriverDetail;
import com.ridesharing.entity.Ride;
import com.ridesharing.entity.RideStatus;
import com.ridesharing.entity.User;
import com.ridesharing.entity.UserRole;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.DriverDetailRepository;
import com.ridesharing.repository.RideRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final DriverDetailRepository driverDetailRepository;
    private final UserService userService;
    private final FreeDistanceCalculatorService freeDistanceCalculatorService;
    private final PaymentService paymentService;

    public RideResponseDto postRide(String phoneNumber, RidePostDto ridePostDto) {
        User driver = userService.getUserByPhoneNumber(phoneNumber);
        
        // Verify user is a driver
        if (!driver.getRole().equals(UserRole.DRIVER)) {
            throw new RuntimeException("Only drivers can post rides");
        }

        // Check if driver has completed their profile
        Optional<DriverDetail> driverDetailOpt = driverDetailRepository.findByUser(driver);
        if (driverDetailOpt.isEmpty()) {
            throw new RuntimeException("Please complete your driver profile before posting rides");
        }

        // Check if driver details are verified
        DriverDetail driverDetail = driverDetailOpt.get();
        if (!driverDetail.getIsVerified()) {
            throw new RuntimeException("Your driver profile must be verified before posting rides");
        }

        // Calculate dynamic fare based on distance using FREE service
        BigDecimal calculatedFare = ridePostDto.getPricePerSeat();
        try {
            log.info("Calculating dynamic fare for route: {} to {} using FREE services", 
                    ridePostDto.getSource(), ridePostDto.getDestination());
            
            DistanceResponseDto distanceResponse = freeDistanceCalculatorService.calculateDistanceAndFare(
                ridePostDto.getSource(), 
                ridePostDto.getDestination()
            );
            
            if ("SUCCESS".equals(distanceResponse.getStatus())) {
                calculatedFare = distanceResponse.getCalculatedFare();
                log.info("Dynamic fare calculated: ₹{} for distance: {} km", 
                        calculatedFare, distanceResponse.getDistanceKm());
            } else {
                log.warn("Failed to calculate dynamic fare: {}. Using provided fare: ₹{}", 
                        distanceResponse.getErrorMessage(), calculatedFare);
            }
        } catch (Exception e) {
            log.error("Error calculating dynamic fare, using provided fare", e);
        }

        // Create ride with auto-filled vehicle details
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource(ridePostDto.getSource());
        ride.setDestination(ridePostDto.getDestination());
        ride.setDepartureDate(ridePostDto.getDepartureDate());
        ride.setAvailableSeats(ridePostDto.getAvailableSeats());
        ride.setTotalSeats(ridePostDto.getAvailableSeats());
        ride.setPricePerSeat(calculatedFare); // Use calculated fare
        ride.setNotes(ridePostDto.getNotes());
        
        // Auto-fill vehicle details from driver profile
        ride.setVehicleModel(driverDetail.getCarModel());
        ride.setVehicleColor(driverDetail.getCarColor());
        ride.setVehicleNumber(driverDetail.getCarNumber());
        ride.setVehicleType(determineVehicleType(driverDetail.getCarModel()));

        Ride savedRide = rideRepository.save(ride);
        return convertToResponseDto(savedRide);
    }

    public Page<RideResponseDto> searchRides(RideSearchDto searchDto) {
        Sort sort = Sort.by(
            searchDto.getSortDirection().equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
            searchDto.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(searchDto.getPage(), searchDto.getSize(), sort);
        
        Page<Ride> rides = rideRepository.searchAvailableRides(
            searchDto.getSource(),
            searchDto.getDestination(),
            searchDto.getDepartureDate(),
            searchDto.getMinSeats(),
            searchDto.getMaxPrice(),
            searchDto.getVehicleType(),
            pageable
        );

        return rides.map(this::convertToResponseDto);
    }

    public List<RideResponseDto> getDriverRides(String phoneNumber) {
        User driver = userService.getUserByPhoneNumber(phoneNumber);
        
        if (!driver.getRole().equals(UserRole.DRIVER)) {
            throw new RuntimeException("Only drivers can view their rides");
        }

        List<Ride> rides = rideRepository.findByDriverOrderByDepartureDateDesc(driver);
        return rides.stream()
                   .map(this::convertToResponseDto)
                   .collect(Collectors.toList());
    }

    public List<RideResponseDto> getUpcomingDriverRides(String phoneNumber) {
        User driver = userService.getUserByPhoneNumber(phoneNumber);
        List<Ride> rides = rideRepository.findUpcomingRidesByDriver(driver);
        return rides.stream()
                   .map(this::convertToResponseDto)
                   .collect(Collectors.toList());
    }

    public RideResponseDto getRideById(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        return convertToResponseDto(ride);
    }

    public RideResponseDto updateRideStatus(String phoneNumber, Long rideId, RideStatus status) {
        User driver = userService.getUserByPhoneNumber(phoneNumber);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("You can only update your own rides");
        }

        ride.setStatus(status);
        Ride updatedRide = rideRepository.save(ride);
        
        // 🚗💰 AUTO-SETTLEMENT: When driver marks ride as COMPLETED
        if (status == RideStatus.COMPLETED) {
            log.info("🚗✅ Ride {} marked as COMPLETED by driver {}. Triggering auto-payment settlement...", 
                    rideId, driver.getId());
            
            // Get all PAID bookings for this ride and settle payments
            List<Booking> paidBookings = bookingRepository.findByRideAndStatusOrderByBookingDateAsc(
                    ride, BookingStatus.PAID);
            
            for (Booking booking : paidBookings) {
                try {
                    // Auto-settle payment for each booking
                    paymentService.autoSettlePaymentOnRideComplete(booking.getId());
                    
                    // Update booking status to COMPLETED
                    booking.setStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(booking);
                    
                    log.info("💰✅ Payment auto-settled for booking {} - Driver gets paid instantly!", 
                            booking.getId());
                            
                } catch (Exception e) {
                    log.error("❌ Failed to auto-settle payment for booking {}: {}", 
                            booking.getId(), e.getMessage());
                    // Continue with other bookings even if one fails
                }
            }
            
            log.info("🎉 Auto-settlement completed for ride {}. Driver earnings processed!", rideId);
        }
        
        return convertToResponseDto(updatedRide);
    }

    public void deleteRide(String phoneNumber, Long rideId) {
        User driver = userService.getUserByPhoneNumber(phoneNumber);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("You can only delete your own rides");
        }

        // Check if ride has bookings
        List<com.ridesharing.entity.Booking> bookings = bookingRepository.findByRideAndStatusOrderByBookingDateAsc(
            ride, com.ridesharing.entity.BookingStatus.CONFIRMED);
        
        if (!bookings.isEmpty()) {
            throw new RuntimeException("Cannot delete ride with confirmed bookings");
        }

        rideRepository.delete(ride);
    }

    private RideResponseDto convertToResponseDto(Ride ride) {
        RideResponseDto dto = new RideResponseDto();
        dto.setId(ride.getId());
        dto.setDriverName(ride.getDriver().getFirstName() + " " + ride.getDriver().getLastName());
        dto.setDriverPhone(ride.getDriver().getPhoneNumber());
        dto.setSource(ride.getSource());
        dto.setDestination(ride.getDestination());
        dto.setDepartureDate(ride.getDepartureDate());
        dto.setAvailableSeats(ride.getAvailableSeats());
        dto.setTotalSeats(ride.getTotalSeats());
        dto.setPricePerSeat(ride.getPricePerSeat());
        dto.setVehicleType(ride.getVehicleType());
        dto.setVehicleModel(ride.getVehicleModel());
        dto.setVehicleColor(ride.getVehicleColor());
        dto.setVehicleNumber(ride.getVehicleNumber());
        dto.setNotes(ride.getNotes());
        dto.setStatus(ride.getStatus());
        dto.setCreatedAt(ride.getCreatedAt());
        dto.setUpdatedAt(ride.getUpdatedAt());
        
        // Calculate booked seats
        Integer bookedSeats = ride.getTotalSeats() - ride.getAvailableSeats();
        dto.setBookedSeats(bookedSeats);
        
        return dto;
    }

    private String determineVehicleType(String carModel) {
        if (carModel == null) return "Car";
        
        String model = carModel.toLowerCase();
        if (model.contains("suv") || model.contains("fortuner") || model.contains("innova")) {
            return "SUV";
        } else if (model.contains("hatchback") || model.contains("swift") || model.contains("i20")) {
            return "Hatchback";
        } else if (model.contains("sedan") || model.contains("city") || model.contains("verna")) {
            return "Sedan";
        }
        return "Car";
    }
}