package com.SwiftBasket.notification_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.SwiftBasket.notification_service.exception.EmailSendException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Development email sender via Mailtrap SMTP. The EmailSender interface keeps
 * this swappable for SES/SendGrid in production without touching callers.
 */
@Service
@Slf4j
public class MailtrapEmailSender implements EmailSender {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom("no-reply@swiftbasket.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} - subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to build email for {} - subject: {}: {}", to, subject, e.getMessage());
            throw new EmailSendException("Failed to build email");
        } catch (Exception e) {
            log.error("Failed to send email to {} - subject: {}: {}", to, subject, e.getMessage());
            throw new EmailSendException("Failed to send email");
        }
    }
}
