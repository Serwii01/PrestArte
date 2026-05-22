package com.prestarte.tfg.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correo electrónico.
 *
 * Encapsula el uso de {@link JavaMailSender} para enviar mensajes
 * sencillos y mensajes con adjunto. Cualquier error de SMTP se
 * registra en el log y se traga, de manera que un problema de
 * credenciales o de red no interrumpa el flujo de negocio que
 * dispara la notificación.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /**
     * Envía un correo de solo texto. Si el envío falla por cualquier
     * motivo, se anota en el log y se sigue ejecutando el flujo.
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email enviado a {}", to);
        } catch (MailException ex) {
            log.warn("No se pudo enviar el email a {}: {}", to, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Error inesperado enviando email a {}: {}", to, ex.getMessage());
        }
    }

    /**
     * Envía un correo HTML con un archivo adjunto, por ejemplo el
     * contrato de un préstamo. Igual que el envío simple, si falla
     * el SMTP el error queda anotado en el log sin propagarse.
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
            log.info("Email con adjunto enviado a {}", to);
        } catch (MessagingException ex) {
            log.warn("Error componiendo el email para {}: {}", to, ex.getMessage());
        } catch (MailException ex) {
            log.warn("Error enviando el email con adjunto a {}: {}", to, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Error inesperado enviando email con adjunto a {}: {}", to, ex.getMessage());
        }
    }
}
