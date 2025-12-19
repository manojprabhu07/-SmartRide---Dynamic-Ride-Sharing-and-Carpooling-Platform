package com.ridesharing.controller;

import com.ridesharing.dto.ReminderTestDto;
import com.ridesharing.entity.*;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.RideRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.service.RideReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test controller for ride reminder system
 * Only available in development/test environments
 */
@RestController
@RequestMapping("/api/test/reminders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@ConditionalOnProperty(
    value = "app.test.controllers.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class ReminderTestController {

    private final RideReminderService reminderService;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    /**
     * Test all reminder scenarios with predefined test data
     * GET /api/test/reminders/scenarios
     */
    @GetMapping("/scenarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testReminderScenarios() {
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            
            // Scenario 1: Booking made 30 minutes before ride (should get 30min reminder)
            results.add(testScenario(
                "30 minutes before ride",
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(30),
                "Should schedule 30-minute reminder"
            ));
            
            // Scenario 2: Booking made 2 hours before ride (should get 1h reminder)
            results.add(testScenario(
                "2 hours before ride",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                "Should schedule 1-hour reminder"
            ));
            
            // Scenario 3: Booking made 12 hours before ride (should get 1h reminder)
            results.add(testScenario(
                "12 hours before ride",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(12),
                "Should schedule 1-hour reminder"
            ));
            
            // Scenario 4: Booking made 2 days before ride (should get 24h + 1h reminders)
            results.add(testScenario(
                "2 days before ride",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                "Should schedule 24-hour and 1-hour reminders"
            ));
            
            // Scenario 5: Booking made 1 week before ride (should get 24h + 1h reminders)
            results.add(testScenario(
                "1 week before ride",
                LocalDateTime.now(),
                LocalDateTime.now().plusWeeks(1),
                "Should schedule 24-hour and 1-hour reminders"
            ));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All reminder scenarios tested",
                "scenarios", results
            ));
            
        } catch (Exception e) {
            log.error("Error testing reminder scenarios", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to test scenarios: " + e.getMessage()
            ));
        }
    }

    /**
     * Test a specific reminder scenario
     */
    private Map<String, Object> testScenario(String scenarioName, LocalDateTime bookingTime, 
                                           LocalDateTime rideTime, String expectedBehavior) {
        try {
            log.info("Testing scenario: {}", scenarioName);
            
            // Create or get test user (passenger)
            User passenger = getOrCreateTestUser("test.passenger@example.com", "Test Passenger", "1234567890");
            
            // Create or get test driver
            User driver = getOrCreateTestDriver("test.driver@example.com", "Test Driver", "0987654321");
            
            // Create test ride
            Ride ride = createTestRide(driver, rideTime);
            
            // Create test booking
            Booking booking = createTestBooking(passenger, ride, bookingTime);
            
            // Calculate time difference
            long hoursDifference = ChronoUnit.HOURS.between(bookingTime, rideTime);
            
            // Schedule reminders
            reminderService.scheduleRemindersForBooking(booking);
            
            // Get scheduled reminders
            var reminders = reminderService.getRemindersForBooking(booking.getId());
            
            return Map.of(
                "scenario", scenarioName,
                "bookingTime", bookingTime.toString(),
                "rideTime", rideTime.toString(),
                "hoursDifference", hoursDifference,
                "expectedBehavior", expectedBehavior,
                "remindersScheduled", reminders.size(),
                "reminderTypes", reminders.stream().map(r -> r.getReminderType().toString()).toList(),
                "success", true
            );
            
        } catch (Exception e) {
            log.error("Error in test scenario: {}", scenarioName, e);
            return Map.of(
                "scenario", scenarioName,
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Create a test scenario with custom times
     * POST /api/test/reminders/custom
     */
    @PostMapping("/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testCustomScenario(@RequestBody ReminderTestDto testDto) {
        try {
            log.info("Testing custom reminder scenario: {}", testDto.getScenario());
            
            // Create or get test user (passenger)
            User passenger = getOrCreateTestUser(testDto.getPassengerEmail(), "Test Passenger", "1234567890");
            
            // Create or get test driver
            User driver = getOrCreateTestUser(testDto.getDriverName() + "@example.com", testDto.getDriverName(), testDto.getDriverPhone());
            
            // Create test ride
            Ride ride = createTestRide(driver, testDto.getRideTime());
            ride.setSource(testDto.getSource());
            ride.setDestination(testDto.getDestination());
            ride = rideRepository.save(ride);
            
            // Create test booking
            Booking booking = createTestBooking(passenger, ride, testDto.getBookingTime());
            booking.setSeatsBooked(testDto.getSeatsBooked());
            booking = bookingRepository.save(booking);
            
            // Schedule reminders
            reminderService.scheduleRemindersForBooking(booking);
            
            // Get scheduled reminders
            var reminders = reminderService.getRemindersForBooking(booking.getId());
            
            long hoursDifference = ChronoUnit.HOURS.between(testDto.getBookingTime(), testDto.getRideTime());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "scenario", testDto.getScenario(),
                "bookingId", booking.getId(),
                "rideId", ride.getId(),
                "hoursDifference", hoursDifference,
                "remindersScheduled", reminders.size(),
                "reminders", reminders.stream().map(r -> Map.of(
                    "id", r.getId(),
                    "type", r.getReminderType().toString(),
                    "scheduledTime", r.getScheduledTime().toString(),
                    "status", r.getStatus().toString()
                )).toList()
            ));
            
        } catch (Exception e) {
            log.error("Error testing custom scenario", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to test custom scenario: " + e.getMessage()
            ));
        }
    }

    /**
     * Manually trigger reminder processing (for testing)
     * POST /api/test/reminders/process
     */
    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerReminderProcessing() {
        try {
            log.info("Manually triggering reminder processing for testing");
            reminderService.processDueReminders();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Reminder processing triggered successfully"
            ));
        } catch (Exception e) {
            log.error("Error triggering reminder processing", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to trigger processing: " + e.getMessage()
            ));
        }
    }

    /**
     * Test email configuration
     * POST /api/test/reminders/email-test
     */
    @PostMapping("/email-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email is required"
                ));
            }
            
            log.info("Testing email to: {}", email);
            
            // Send simple test email
            reminderService.sendTestEmail(email, "Test from Ride Reminder System", 
                "This is a test email to verify your email configuration is working correctly.\n\n" +
                "If you receive this, the system can send emails successfully!\n\n" +
                "Timestamp: " + LocalDateTime.now());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test email sent successfully to: " + email
            ));
            
        } catch (Exception e) {
            log.error("Email test failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Email test failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Test immediate 30-minute scenario
     * POST /api/test/reminders/immediate-test
     */
    @PostMapping("/immediate-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testImmediateScenario() {
        try {
            log.info("Testing immediate 30-minute scenario");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime rideTimeIn30Min = now.plusMinutes(30);
            
            // Create test user and driver
            User passenger = getOrCreateTestUser("immediate.test@example.com", "Immediate Passenger", "9999999999");
            User driver = getOrCreateTestDriver("immediate.driver@example.com", "Immediate Driver", "8888888888");
            
            // Create test ride
            Ride ride = createTestRide(driver, rideTimeIn30Min);
            ride.setSource("Current Location");
            ride.setDestination("Nearby Mall");
            ride = rideRepository.save(ride);
            
            // Create test booking with current time
            Booking booking = createTestBooking(passenger, ride, now);
            booking = bookingRepository.save(booking);
            
            log.info("Created booking {} for immediate test. Booking time: {}, Ride time: {}", 
                    booking.getId(), now, rideTimeIn30Min);
            
            // Schedule reminders
            reminderService.scheduleRemindersForBooking(booking);
            
            // Get scheduled reminders
            var reminders = reminderService.getRemindersForBooking(booking.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "scenario", "Immediate 30-minute test",
                "bookingId", booking.getId(),
                "rideId", ride.getId(),
                "bookingTime", now.toString(),
                "rideTime", rideTimeIn30Min.toString(),
                "currentTime", LocalDateTime.now().toString(),
                "remindersScheduled", reminders.size(),
                "reminders", reminders.stream().map(r -> Map.of(
                    "id", r.getId(),
                    "type", r.getReminderType().toString(),
                    "scheduledTime", r.getScheduledTime().toString(),
                    "status", r.getStatus().toString(),
                    "recipientEmail", r.getRecipientEmail()
                )).toList()
            ));
            
        } catch (Exception e) {
            log.error("Error in immediate test scenario", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to test immediate scenario: " + e.getMessage()
            ));
        }
    }

    /**
     * Clean up test data
     * DELETE /api/test/reminders/cleanup
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cleanupTestData() {
        try {
            log.info("Cleaning up test data");
            
            // Delete test bookings
            var testBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getPassenger().getEmail().contains("test."))
                .toList();
            bookingRepository.deleteAll(testBookings);
            
            // Delete test rides
            var testRides = rideRepository.findAll().stream()
                .filter(r -> r.getDriver().getEmail().contains("test."))
                .toList();
            rideRepository.deleteAll(testRides);
            
            // Delete test users
            var testUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail().contains("test."))
                .toList();
            userRepository.deleteAll(testUsers);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test data cleaned up successfully",
                "deletedBookings", testBookings.size(),
                "deletedRides", testRides.size(),
                "deletedUsers", testUsers.size()
            ));
        } catch (Exception e) {
            log.error("Error cleaning up test data", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to cleanup: " + e.getMessage()
            ));
        }
    }

    private User getOrCreateTestUser(String email, String name, String phone) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> {
                User user = new User();
                user.setEmail(email);
                user.setFirstName(name.split(" ")[0]);
                user.setLastName(name.split(" ").length > 1 ? name.split(" ")[1] : "");
                user.setPhoneNumber(phone);
                user.setPassword("test123"); // Test password
                user.setRole(UserRole.USER); // Changed from PASSENGER to USER
                return userRepository.save(user);
            });
    }

    private User getOrCreateTestDriver(String email, String name, String phone) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> {
                User driver = new User();
                driver.setEmail(email);
                driver.setFirstName(name.split(" ")[0]);
                driver.setLastName(name.split(" ").length > 1 ? name.split(" ")[1] : "");
                driver.setPhoneNumber(phone);
                driver.setPassword("test123"); // Test password
                driver.setRole(UserRole.DRIVER); // This should be correct
                return userRepository.save(driver);
            });
    }

    private Ride createTestRide(User driver, LocalDateTime departureTime) {
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource("Test Source");
        ride.setDestination("Test Destination");
        ride.setDepartureDate(departureTime);
        ride.setAvailableSeats(3);
        ride.setTotalSeats(4);
        ride.setPricePerSeat(BigDecimal.valueOf(100.00));
        ride.setVehicleType("Sedan");
        ride.setVehicleModel("Test Car");
        ride.setVehicleColor("White");
        ride.setVehicleNumber("TEST-1234");
        ride.setStatus(RideStatus.ACTIVE);
        return rideRepository.save(ride);
    }

    private Booking createTestBooking(User passenger, Ride ride, LocalDateTime bookingTime) {
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeatsBooked(1);
        booking.setTotalAmount(ride.getPricePerSeat());
        booking.setPassengerName(passenger.getFirstName() + " " + passenger.getLastName());
        booking.setPassengerPhone(passenger.getPhoneNumber());
        booking.setStatus(BookingStatus.CONFIRMED); // Confirmed to trigger reminders
        booking.setBookingDate(bookingTime);
        return bookingRepository.save(booking);
    }
}