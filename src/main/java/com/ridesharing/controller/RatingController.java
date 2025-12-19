package com.ridesharing.controller;

import com.ridesharing.dto.ApiResponse;
import com.ridesharing.dto.RatingRequestDto;
import com.ridesharing.dto.RatingResponseDto;
import com.ridesharing.dto.RatingSummaryDto;
import com.ridesharing.service.RatingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RatingController {

    private final RatingService ratingService;

    /**
     * Create a new rating (Passenger to Driver only)
     */
    @PostMapping("/passenger/{passengerId}")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createRating(
            @PathVariable Long passengerId,
            @Valid @RequestBody RatingRequestDto ratingRequest) {
        try {
            log.info("Creating rating for passenger: {}", passengerId);
            RatingResponseDto rating = ratingService.createRating(passengerId, ratingRequest);
            ApiResponse response = new ApiResponse("SUCCESS", "Rating created successfully", rating);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating rating: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Get all ratings for a driver
     */
    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getRatingsForDriver(@PathVariable Long driverId) {
        try {
            log.info("Fetching ratings for driver: {}", driverId);
            List<RatingResponseDto> ratings = ratingService.getRatingsForDriver(driverId);
            ApiResponse response = new ApiResponse("SUCCESS", "Ratings retrieved successfully", ratings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching ratings for driver: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Get all ratings by a passenger
     */
    @GetMapping("/passenger/{passengerId}")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getRatingsByPassenger(@PathVariable Long passengerId) {
        try {
            log.info("Fetching ratings by passenger: {}", passengerId);
            List<RatingResponseDto> ratings = ratingService.getRatingsByPassenger(passengerId);
            ApiResponse response = new ApiResponse("SUCCESS", "Ratings retrieved successfully", ratings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching ratings by passenger: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Get driver rating summary
     */
    @GetMapping("/driver/{driverId}/summary")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getDriverRatingSummary(@PathVariable Long driverId) {
        try {
            log.info("Fetching rating summary for driver: {}", driverId);
            RatingSummaryDto summary = ratingService.getDriverRatingSummary(driverId);
            ApiResponse response = new ApiResponse("SUCCESS", "Rating summary retrieved successfully", summary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching driver rating summary: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Get paginated ratings for a driver
     */
    @GetMapping("/driver/{driverId}/paginated")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getPaginatedRatingsForDriver(
            @PathVariable Long driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Fetching paginated ratings for driver: {}", driverId);
            Pageable pageable = PageRequest.of(page, size);
            Page<RatingResponseDto> ratingsPage = ratingService.getRatingsForDriverPaginated(driverId, pageable);
            ApiResponse response = new ApiResponse("SUCCESS", "Paginated ratings retrieved successfully", ratingsPage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching paginated ratings for driver: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Get ratings with comments for a driver
     */
    @GetMapping("/driver/{driverId}/comments")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getRatingsWithComments(@PathVariable Long driverId) {
        try {
            log.info("Fetching ratings with comments for driver: {}", driverId);
            List<RatingResponseDto> ratings = ratingService.getRatingsWithComments(driverId);
            ApiResponse response = new ApiResponse("SUCCESS", "Ratings with comments retrieved successfully", ratings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching ratings with comments: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Get a specific rating by ID
     */
    @GetMapping("/{ratingId}")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getRatingById(@PathVariable Long ratingId) {
        try {
            log.info("Fetching rating with id: {}", ratingId);
            RatingResponseDto rating = ratingService.getRatingById(ratingId);
            ApiResponse response = new ApiResponse("SUCCESS", "Rating retrieved successfully", rating);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching rating by id: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Update a rating
     */
    @PutMapping("/{ratingId}")
    @PreAuthorize("hasRole('USER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateRating(
            @PathVariable Long ratingId,
            @Valid @RequestBody RatingRequestDto ratingRequest) {
        try {
            log.info("Updating rating with id: {}", ratingId);
            RatingResponseDto updatedRating = ratingService.updateRating(ratingId, ratingRequest);
            ApiResponse response = new ApiResponse("SUCCESS", "Rating updated successfully", updatedRating);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating rating: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Delete a rating
     */
    @DeleteMapping("/{ratingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteRating(@PathVariable Long ratingId) {
        try {
            log.info("Deleting rating with id: {}", ratingId);
            ratingService.deleteRating(ratingId);
            ApiResponse response = new ApiResponse("SUCCESS", "Rating deleted successfully", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting rating: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * Get all ratings (Admin only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Admin fetching all ratings");
            Pageable pageable = PageRequest.of(page, size);
            Page<RatingResponseDto> ratingsPage = ratingService.getAllRatingsPaginated(pageable);
            ApiResponse response = new ApiResponse("SUCCESS", "All ratings retrieved successfully", ratingsPage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all ratings: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("ERROR", e.getMessage(), null));
        }
    }
}