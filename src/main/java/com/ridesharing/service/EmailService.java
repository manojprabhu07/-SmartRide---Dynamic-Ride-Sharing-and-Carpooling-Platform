package com.ridesharing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.ridesharing.entity.Booking;
import com.ridesharing.entity.Ride;
import com.ridesharing.entity.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.support}")
    private String supportEmail;

    public void sendBookingConfirmationEmail(User passenger, Booking booking, Ride ride, User driver) {
        try {
            String subject = "Booking Confirmed - SmartRide";
            String templateName = "booking-confirmed";
            
            Map<String, Object> templateModel = createBookingEmailModel(passenger, booking, ride, driver);
            templateModel.put("status", "CONFIRMED");
            templateModel.put("statusMessage", "Your booking has been confirmed by the driver!");
            templateModel.put("statusColor", "#10B981"); // Green color
            
            sendTemplateEmail(passenger.getEmail(), subject, templateName, templateModel);
            log.info("Booking confirmation email sent to: {}", passenger.getEmail());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to: {}", passenger.getEmail(), e);
        }
    }

    public void sendBookingCancellationEmail(User passenger, Booking booking, Ride ride, User driver) {
        try {
            String subject = "Booking Cancelled - SmartRide";
            String templateName = "booking-cancelled";
            
            Map<String, Object> templateModel = createBookingEmailModel(passenger, booking, ride, driver);
            templateModel.put("status", "CANCELLED");
            templateModel.put("statusMessage", "Your booking has been cancelled by the driver.");
            templateModel.put("statusColor", "#EF4444"); // Red color
            
            sendTemplateEmail(passenger.getEmail(), subject, templateName, templateModel);
            log.info("Booking cancellation email sent to: {}", passenger.getEmail());
        } catch (Exception e) {
            log.error("Failed to send booking cancellation email to: {}", passenger.getEmail(), e);
        }
    }

    public void sendBookingStatusUpdateEmail(User passenger, Booking booking, Ride ride, User driver, String status) {
        try {
            String subject = String.format("Booking %s - SmartRide", status);
            String templateName = "booking-status-update";
            
            Map<String, Object> templateModel = createBookingEmailModel(passenger, booking, ride, driver);
            templateModel.put("status", status);
            templateModel.put("statusMessage", getStatusMessage(status));
            templateModel.put("statusColor", getStatusColor(status));
            
            sendTemplateEmail(passenger.getEmail(), subject, templateName, templateModel);
            log.info("Booking status update email sent to: {} for status: {}", passenger.getEmail(), status);
        } catch (Exception e) {
            log.error("Failed to send booking status update email to: {}", passenger.getEmail(), e);
        }
    }

    private Map<String, Object> createBookingEmailModel(User passenger, Booking booking, Ride ride, User driver) {
        Map<String, Object> model = new HashMap<>();
        
        // Passenger details
        model.put("passengerName", passenger.getFirstName() + " " + passenger.getLastName());
        model.put("passengerPhone", passenger.getPhoneNumber());
        
        // Driver details
        model.put("driverName", driver.getFirstName() + " " + driver.getLastName());
        model.put("driverPhone", driver.getPhoneNumber());
        
        // Booking details
        model.put("bookingId", booking.getId());
        model.put("bookingDate", booking.getBookingDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        model.put("seatsBooked", booking.getSeatsBooked());
        model.put("totalAmount", String.format("₹%.2f", booking.getTotalAmount()));
        
        // Ride details
        model.put("rideId", ride.getId());
        model.put("source", ride.getSource());
        model.put("destination", ride.getDestination());
        model.put("departureDate", ride.getDepartureDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        model.put("departureTime", ride.getDepartureDate().format(DateTimeFormatter.ofPattern("hh:mm a")));
        model.put("pricePerSeat", String.format("₹%.2f", ride.getPricePerSeat()));
        model.put("vehicleInfo", ride.getVehicleNumber() + " (" + ride.getVehicleType() + ")");
        
        // Support details
        model.put("supportEmail", supportEmail);
        model.put("currentYear", java.time.Year.now().getValue());
        
        return model;
    }

    private String getStatusMessage(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> "Your booking has been confirmed by the driver!";
            case "CANCELLED" -> "Your booking has been cancelled by the driver.";
            case "COMPLETED" -> "Your ride has been completed successfully!";
            case "PENDING" -> "Your booking is pending driver confirmation.";
            default -> "Your booking status has been updated.";
        };
    }

    private String getStatusColor(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> "#10B981"; // Green
            case "CANCELLED" -> "#EF4444"; // Red
            case "COMPLETED" -> "#3B82F6"; // Blue
            case "PENDING" -> "#F59E0B"; // Amber
            default -> "#6B7280"; // Gray
        };
    }

    private void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> templateModel) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Create Thymeleaf context
            Context context = new Context();
            context.setVariables(templateModel);

            // Process the template
            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            emailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendRideReminderEmail(String to, String subject, Map<String, Object> templateModel) {
        try {
            // Add current year and support email if not present
            templateModel.putIfAbsent("currentYear", java.time.Year.now().getValue());
            templateModel.putIfAbsent("supportEmail", supportEmail);
            
            sendTemplateEmail(to, subject, "ride-reminder", templateModel);
            log.info("Ride reminder email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send ride reminder email to: {}", to, e);
            throw new RuntimeException("Failed to send ride reminder email", e);
        }
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            emailSender.send(message);
            log.info("Simple email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}