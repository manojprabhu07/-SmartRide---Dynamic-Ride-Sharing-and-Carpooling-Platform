package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingSummaryDto {

    private Long userId;
    private String userName;
    private String userEmail;
    private Double averageRating;
    private Long totalRatings;
    private Long fiveStarCount;
    private Long fourStarCount;
    private Long threeStarCount;
    private Long twoStarCount;
    private Long oneStarCount;
    private List<RatingResponseDto> recentRatings;
}