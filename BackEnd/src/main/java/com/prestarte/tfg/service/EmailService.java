package com.prestarte.tfg.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envía un email con el contrato PDF adjunto.
     */
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true indica que es un mensaje "multipart" (con adjuntos)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true para permitir HTML en el cuerpo

            // Añadimos el PDF desde el array de bytes que genera nuestro PdfGeneratorService
            helper.addAttachment(fileName, new ByteArrayResource(attachment));

            mailSender.send(message);
            System.out.println("Email enviado con éxito a: " + to);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el email: " + e.getMessage());
        }
    }
}