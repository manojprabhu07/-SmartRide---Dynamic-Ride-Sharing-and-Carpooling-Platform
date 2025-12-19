package com.ridesharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminProfileDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}