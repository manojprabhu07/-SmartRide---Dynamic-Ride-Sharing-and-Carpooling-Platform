package com.ridesharing.dto;

import lombok.Data;
import com.ridesharing.entity.RideStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class RideResponseDto {

    private Long id;
    private String driverName;
    private String driverPhone;
    private String source;
    private String destination;
    private LocalDateTime departureDate;
    private Integer availableSeats;
    private Integer totalSeats;
    private BigDecimal pricePerSeat;
    private String vehicleType;
    private String vehicleModel;
    private String vehicleColor;
    private String vehicleNumber;
    private String notes;
    private RideStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer bookedSeats;
}