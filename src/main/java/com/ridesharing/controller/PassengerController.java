package com.ridesharing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ridesharing.dto.ApiResponse;
import com.ridesharing.dto.BookingDto;
import com.ridesharing.dto.BookingResponseDto;
import com.ridesharing.security.JwtTokenProvider;
import com.ridesharing.service.BookingService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class PassengerController {

    private final BookingService bookingService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ResponseEntity<ApiResponse> bookRide(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody BookingDto bookingDto) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            BookingResponseDto booking = bookingService.bookRide(phoneNumber, bookingDto);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Ride booked successfully",
                booking
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while booking the ride",
                null
            ));
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse> getMyBookings(
            @RequestHeader("Authorization") String token) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            List<BookingResponseDto> bookings = bookingService.getPassengerBookings(phoneNumber);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Bookings retrieved successfully",
                bookings
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while retrieving bookings",
                null
            ));
        }
    }

    @GetMapping("/my-bookings/upcoming")
    public ResponseEntity<ApiResponse> getMyUpcomingBookings(
            @RequestHeader("Authorization") String token) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            List<BookingResponseDto> bookings = bookingService.getUpcomingBookings(phoneNumber);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Upcoming bookings retrieved successfully",
                bookings
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while retrieving upcoming bookings",
                null
            ));
        }
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse> cancelBooking(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookingId) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            BookingResponseDto booking = bookingService.cancelBooking(phoneNumber, bookingId);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Booking cancelled successfully",
                booking
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while cancelling the booking",
                null
            ));
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse> getBookingById(
            @PathVariable Long bookingId) {
        try {
            BookingResponseDto booking = bookingService.getBookingById(bookingId);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Booking details retrieved successfully",
                booking
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                "ERROR",
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while retrieving booking details",
                null
            ));
        }
    }

    // Driver can also view bookings for their rides
    @GetMapping("/driver-bookings")
    public ResponseEntity<ApiResponse> getDriverBookings(
            @RequestHeader("Authorization") String token) {
        try {
            String phoneNumber = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            List<BookingResponseDto> bookings = bookingService.getDriverBookings(phoneNumber);
            
            return ResponseEntity.ok(new ApiResponse(
                "SUCCESS",
                "Driver bookings retrieved successfully",
                bookings
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(
                "ERROR",
                "An error occurred while retrieving driver bookings",
                null
            ));
        }
    }
}