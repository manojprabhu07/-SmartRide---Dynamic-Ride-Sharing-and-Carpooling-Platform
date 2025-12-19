package com.ridesharing.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class RideSearchDto {

    private String source;
    private String destination;
    private LocalDateTime departureDate;
    private Integer minSeats;
    private BigDecimal maxPrice;
    private String vehicleType;
    
    // For pagination
    private Integer page = 0;
    private Integer size = 10;
    
    // For sorting
    private String sortBy = "departureDate";
    private String sortDirection = "ASC";
}