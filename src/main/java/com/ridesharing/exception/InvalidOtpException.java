package com.ridesharing.exception;

public class InvalidOtpException extends RuntimeException {
    
    public InvalidOtpException(String message) {
        super(message);
    }
    
    public InvalidOtpException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InvalidOtpException() {
        super("Invalid or expired OTP");
    }
}