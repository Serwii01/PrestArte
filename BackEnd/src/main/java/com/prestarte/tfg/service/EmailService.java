package com.prestarte.tfg.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a basic text email without attachments.
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("Notification email sent to: " + to);
        } catch (Exception e) {
            System.err.println("Error sending simple email: " + e.getMessage());
        }
    }

    /**
     * Sends an email with a PDF contract attached.
     */
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            helper.addAttachment(fileName, new ByteArrayResource(attachment));

            mailSender.send(message);
            System.out.println("Email with attachment sent successfully to: " + to);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el email con adjunto: " + e.getMessage());
        }
    }
}