package com.ridesharing.service;

import com.ridesharing.dto.RatingRequestDto;
import com.ridesharing.dto.RatingResponseDto;
import com.ridesharing.dto.RatingSummaryDto;
import com.ridesharing.entity.Rating;
import com.ridesharing.entity.User;
import com.ridesharing.repository.RatingRepository;
import com.ridesharing.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    @Transactional
    public RatingResponseDto createRating(Long passengerId, RatingRequestDto ratingRequest) {
        log.info("Creating rating for passenger: {} and driver: {}", passengerId, ratingRequest.getDriverId());

        // Validate passenger exists
        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));

        // Validate driver exists
        User driver = userRepository.findById(ratingRequest.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + ratingRequest.getDriverId()));

        // Check if rating already exists for this booking (if booking ID is provided)
        if (ratingRequest.getBookingId() != null) {
            boolean exists = ratingRepository.existsByPassengerAndDriverAndBookingId(
                    passenger, driver, ratingRequest.getBookingId());
            if (exists) {
                throw new RuntimeException("Rating already exists for this booking");
            }
        }

        // Create new rating
        Rating rating = Rating.builder()
                .rating(ratingRequest.getRating())
                .comment(ratingRequest.getComment())
                .passenger(passenger)
                .driver(driver)
                .bookingId(ratingRequest.getBookingId())
                .build();

        Rating savedRating = ratingRepository.save(rating);
        log.info("Rating created successfully with id: {}", savedRating.getId());

        return convertToResponseDto(savedRating);
    }

    @Transactional(readOnly = true)
    public List<RatingResponseDto> getRatingsForDriver(Long driverId) {
        log.info("Fetching ratings for driver: {}", driverId);

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        List<Rating> ratings = ratingRepository.findByDriverOrderByCreatedAtDesc(driver);
        return ratings.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RatingResponseDto> getRatingsByPassenger(Long passengerId) {
        log.info("Fetching ratings by passenger: {}", passengerId);

        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));

        List<Rating> ratings = ratingRepository.findByPassengerOrderByCreatedAtDesc(passenger);
        return ratings.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RatingSummaryDto getDriverRatingSummary(Long driverId) {
        log.info("Fetching rating summary for driver: {}", driverId);

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        // Get average rating
        Double averageRating = ratingRepository.getAverageRatingForDriver(driver);
        if (averageRating == null) {
            averageRating = 0.0;
        }

        // Get total ratings count
        Long totalRatings = ratingRepository.getRatingCountForDriver(driver);

        // Get star distribution
        Long fiveStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(driver, 5).size();
        Long fourStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(driver, 4).size();
        Long threeStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(driver, 3).size();
        Long twoStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(driver, 2).size();
        Long oneStarCount = (long) ratingRepository.findByDriverAndRatingOrderByCreatedAtDesc(driver, 1).size();

        // Get recent ratings
        List<Rating> recentRatings = ratingRepository.getRecentRatingsForDriver(driver);
        List<RatingResponseDto> recentRatingDtos = recentRatings.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        return RatingSummaryDto.builder()
                .userId(driver.getId())
                .userName(driver.getFirstName() + " " + driver.getLastName())
                .userEmail(driver.getEmail())
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .recentRatings(recentRatingDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<RatingResponseDto> getRatingsForDriverPaginated(Long driverId, Pageable pageable) {
        log.info("Fetching paginated ratings for driver: {}", driverId);

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        Page<Rating> ratingsPage = ratingRepository.findByDriverOrderByCreatedAtDesc(driver, pageable);

        return ratingsPage.map(this::convertToResponseDto);
    }

    @Transactional(readOnly = true)
    public List<RatingResponseDto> getRatingsWithComments(Long driverId) {
        log.info("Fetching ratings with comments for driver: {}", driverId);

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        List<Rating> ratings = ratingRepository.getRatingsWithCommentsForDriver(driver);
        return ratings.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RatingResponseDto getRatingById(Long ratingId) {
        log.info("Fetching rating with id: {}", ratingId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found with id: " + ratingId));

        return convertToResponseDto(rating);
    }

    @Transactional
    public RatingResponseDto updateRating(Long ratingId, RatingRequestDto ratingRequest) {
        log.info("Updating rating with id: {}", ratingId);

        Rating existingRating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found with id: " + ratingId));

        // Update fields
        existingRating.setRating(ratingRequest.getRating());
        existingRating.setComment(ratingRequest.getComment());

        Rating updatedRating = ratingRepository.save(existingRating);
        log.info("Rating updated successfully with id: {}", updatedRating.getId());

        return convertToResponseDto(updatedRating);
    }

    @Transactional
    public void deleteRating(Long ratingId) {
        log.info("Deleting rating with id: {}", ratingId);

        if (!ratingRepository.existsById(ratingId)) {
            throw new RuntimeException("Rating not found with id: " + ratingId);
        }

        ratingRepository.deleteById(ratingId);
        log.info("Rating deleted successfully with id: {}", ratingId);
    }

    @Transactional(readOnly = true)
    public Page<RatingResponseDto> getAllRatingsPaginated(Pageable pageable) {
        log.info("Fetching all ratings with pagination");

        Page<Rating> ratingsPage = ratingRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ratingsPage.map(this::convertToResponseDto);
    }

    private RatingResponseDto convertToResponseDto(Rating rating) {
        return RatingResponseDto.builder()
                .id(rating.getId())
                .rating(rating.getRating())
                .comment(rating.getComment())
                .passengerId(rating.getPassenger().getId())
                .passengerName(rating.getPassenger().getFirstName() + " " + rating.getPassenger().getLastName())
                .passengerEmail(rating.getPassenger().getEmail())
                .driverId(rating.getDriver().getId())
                .driverName(rating.getDriver().getFirstName() + " " + rating.getDriver().getLastName())
                .driverEmail(rating.getDriver().getEmail())
                .bookingId(rating.getBookingId())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}