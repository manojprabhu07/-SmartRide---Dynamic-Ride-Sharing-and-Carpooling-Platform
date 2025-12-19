package com.ridesharing.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.stereotype.Service;

import com.ridesharing.config.TwilioConfig;

@Service
public class TwilioService {

    private final TwilioConfig twilioConfig;

    public TwilioService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public void sendOtp(String phoneNumber, String otp) {
        try {
            String messageBody = String.format("Your RideSharing verification code is: %s. This code will expire in 5 minutes.", otp);
            
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioConfig.getFromNumber()),
                    messageBody
            ).create();

            System.out.println("OTP sent successfully. Message SID: " + message.getSid());
        } catch (Exception e) {
            System.err.println("Failed to send OTP: " + e.getMessage());
            // In production, you might want to throw a custom exception or handle this differently
        }
    }

    public void sendSms(String phoneNumber, String message) {
        try {
            Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioConfig.getFromNumber()),
                    message
            ).create();

            System.out.println("SMS sent successfully to: " + phoneNumber);
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }
    }
}