package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponseDto {

    private Long id;
    private Integer rating;
    private String comment;
    private Long passengerId;
    private String passengerName;
    private String passengerEmail;
    private Long driverId;
    private String driverName;
    private String driverEmail;
    private Long bookingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}