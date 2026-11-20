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

    private static final int MAX_ATTEMPTS = 3;
    private static final long RATE_LIMIT_BACKOFF_MS = 2000;

    @Autowired
    private JavaMailSender mailSender;

    // Mailtrap rejects concurrent sends from parallel Kafka listeners with 550 "Too many emails per second"
    @Override
    public synchronized void send(String to, String subject, String htmlBody) {
        MimeMessage message;
        try {
            message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom("no-reply@swiftbasket.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
        } catch (MessagingException e) {
            log.error("Failed to build email for {} - subject: {}: {}", to, subject, e.getMessage());
            throw new EmailSendException("Failed to build email");
        }

        for (int attempt = 1; ; attempt++) {
            try {
                mailSender.send(message);
                log.info("Email sent to {} - subject: {}", to, subject);
                return;
            } catch (Exception e) {
                boolean rateLimited = String.valueOf(e.getMessage()).contains("Too many emails");
                if (!rateLimited || attempt >= MAX_ATTEMPTS) {
                    log.error("Failed to send email to {} - subject: {}: {}", to, subject, e.getMessage());
                    throw new EmailSendException("Failed to send email");
                }
                log.warn("Mailtrap rate limit hit for {} - retrying (attempt {}/{})", to, attempt + 1, MAX_ATTEMPTS);
                try {
                    Thread.sleep(RATE_LIMIT_BACKOFF_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new EmailSendException("Failed to send email");
                }
            }
        }
    }
}
