package com.ridesharing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.twilio.Twilio;

@Configuration
@ConfigurationProperties(prefix = "twilio")
public class TwilioConfig {

    private String accountSid;
    private String authToken;
    private String fromNumber;

    public TwilioConfig() {
    }

    @Bean
    public String twilioInit() {
        System.out.println("Initializing Twilio with AccountSid: " + 
                          (accountSid != null ? accountSid.substring(0, 4) + "***" : "null"));
        System.out.println("AuthToken present: " + (authToken != null && !authToken.isEmpty()));
        System.out.println("FromNumber: " + fromNumber);
        
        try {
            if (accountSid != null && authToken != null && !accountSid.isEmpty() && !authToken.isEmpty()) {
                Twilio.init(accountSid, authToken);
                System.out.println("Twilio initialized successfully!");
                return "Twilio initialized successfully";
            } else {
                System.err.println("Twilio not initialized - missing credentials (will continue without SMS)");
                System.err.println("AccountSid: " + accountSid);
                System.err.println("AuthToken: " + (authToken != null ? "present" : "null"));
                return "Twilio not initialized - missing credentials";
            }
        } catch (Exception e) {
            System.err.println("Twilio initialization failed: " + e.getMessage());
            return "Twilio initialization failed: " + e.getMessage();
        }
    }

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }
}