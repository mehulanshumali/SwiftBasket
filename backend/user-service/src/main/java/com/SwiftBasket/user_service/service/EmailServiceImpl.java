package com.SwiftBasket.user_service.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Override
    public void sendPasswordResetEmail(String email, String fullName, String resetToken) {
        try {
            String resetLink = appBaseUrl + "/reset-password?token=" + resetToken;
            String htmlContent = buildPasswordResetEmailTemplate(fullName, resetLink);
            sendEmail(email, "Reset Your SwiftBasket Account Password", htmlContent);
            log.info("Password reset email sent to: {}", email);
        } catch (IOException e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);

        }
    }

    @Override
    public void sendRegistrationConfirmationEmail(String email, String fullName) {
        try {
            String htmlContent = buildRegistrationEmailTemplate(fullName);
            sendEmail(email, "Welcome to SwiftBasket", htmlContent);
            log.info("Registration confirmation email sent to: {}", email);
        } catch (IOException e) {
            log.error("Failed to send registration confirmation email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send registration confirmation email", e);
        }
    }

    private void sendEmail(String email, String subject, String htmlContent) throws IOException {
        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail, fromName));
        
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(email));
        personalization.setSubject(subject);
        mail.addPersonalization(personalization);
        mail.addContent(new Content("text/html", htmlContent));
        
        SendGrid sendGrid = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(com.sendgrid.Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        
        Response response = sendGrid.api(request);
        
        int responseStatusCode = response.getStatusCode();
        if (responseStatusCode >= 200 && responseStatusCode < 300) {
            log.info("Email sent successfully to: {}", email);
        } else {
            log.error("Failed to send email to: {}. Status Code: {}, Body: {}", email, responseStatusCode, response.getBody());
            throw new RuntimeException("Failed to send email");
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
                "            <p class='warning'>⚠️ This link will expire in 1 hour.</p>" +
                "            <p>If you didn't request a password reset, please ignore this email. Your account is still secure.</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>&copy; 2024 SwiftBasket. All rights reserved.</p>" +
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
                "            <p>&copy; 2024 SwiftBasket. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

}
