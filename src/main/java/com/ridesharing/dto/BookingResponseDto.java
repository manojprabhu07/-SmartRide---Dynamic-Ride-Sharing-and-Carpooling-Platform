package com.ridesharing.dto;

import lombok.Data;
import com.ridesharing.entity.BookingStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class BookingResponseDto {

    private Long id;
    private Long rideId;
    private String source;
    private String destination;
    private LocalDateTime departureDate;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private Integer seatsBooked;
    private BigDecimal totalAmount;
    private String passengerName;
    private String passengerPhone;
    private String pickupPoint;
    private BookingStatus status;
    private LocalDateTime bookingDate;
    private LocalDateTime updatedAt;
    
    // Vehicle details
    private String vehicleModel;
    private String vehicleColor;
    private String vehicleNumber;
    private String vehicleMake;
    private BigDecimal pricePerSeat;
}