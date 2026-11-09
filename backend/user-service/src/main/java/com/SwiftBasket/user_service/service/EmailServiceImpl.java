package com.SwiftBasket.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.SwiftBasket.user_service.exception.EmailSendException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends transactional emails via Mailtrap SMTP (development inbox). The
 * EmailService interface keeps this swappable for a production provider
 * (SES/SendGrid) without touching callers.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendPasswordResetEmail(String email, String fullName, String resetToken) {
        String resetLink = "http://localhost:8081" + "/reset-password?token=" + resetToken;
        String htmlContent = buildPasswordResetEmailTemplate(fullName, resetLink);
        sendEmail(email, "Reset Your SwiftBasket Account Password", htmlContent);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    public void sendRegistrationConfirmationEmail(String email, String fullName) {
        String htmlContent = buildRegistrationEmailTemplate(fullName);
        sendEmail(email, "Welcome to SwiftBasket", htmlContent);
        log.info("Registration confirmation email sent to: {}", email);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom("no-reply@swiftbasket.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to build email to {}: {}", to, e.getMessage());
            throw new EmailSendException("Failed to build email");
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new EmailSendException("Failed to send email");
        }
    }

    private String buildPasswordResetEmailTemplate(String fullName, String resetLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4; }" +
                "        .header { background-color: #007bff; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
                "        .content { background-color: white; padding: 20px; }" +
                "        .footer { background-color: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; color: #666; }" +
                "        .btn { display: inline-block; background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                "        .warning { color: #d9534f; font-weight: bold; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>SwiftBasket Password Reset</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Hello " + (fullName != null ? fullName : "User") + ",</p>" +
                "            <p>We received a request to reset your SwiftBasket password. Click the button below to create a new password.</p>" +
                "            <a href='" + resetLink + "' class='btn'>Reset Password</a>" +
                "            <p>Or copy and paste this link in your browser:</p>" +
                "            <p>" + resetLink + "</p>" +
                "            <p class='warning'>This link will expire in 1 hour.</p>" +
                "            <p>If you didn't request a password reset, please ignore this email. Your account is still secure.</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>SwiftBasket. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    private String buildRegistrationEmailTemplate(String fullName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4; }" +
                "        .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
                "        .content { background-color: white; padding: 20px; }" +
                "        .footer { background-color: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; color: #666; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Welcome to SwiftBasket!</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Hello " + (fullName != null ? fullName : "User") + ",</p>" +
                "            <p>Thank you for registering with SwiftBasket! Your account has been successfully created.</p>" +
                "            <p>You can now:</p>" +
                "            <ul>" +
                "                <li>Browse our amazing product catalog</li>" +
                "                <li>Add items to your shopping cart</li>" +
                "                <li>Proceed to checkout securely</li>" +
                "                <li>Track your orders in real-time</li>" +
                "            </ul>" +
                "            <p>If you have any questions, feel free to contact our support team.</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>SwiftBasket. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}
