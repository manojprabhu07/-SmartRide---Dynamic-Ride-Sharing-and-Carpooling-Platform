package com.ridesharing.dto;

public class OtpVerificationDto {

    private String phoneNumber;
    private String otp;

    // Constructors
    public OtpVerificationDto() {}

    public OtpVerificationDto(String phoneNumber, String otp) {
        this.phoneNumber = phoneNumber;
        this.otp = otp;
    }

    // Getters and Setters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}