package com.himalaya.sabor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendActivationEmail(String toEmail, String activationLink) {
        log.info("--------------------------------------------------------------------------------");
        log.info("ENVÍO DE CORREO DE ACTIVACIÓN");
        log.info("Destinatario: {}", toEmail);
        log.info("Enlace de activación: {}", activationLink);
        log.info("--------------------------------------------------------------------------------");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@sabordelhimalaya.com");
            message.setTo(toEmail);
            message.setSubject("Activación de cuenta - Sabor del Himalaya");
            message.setText("¡Bienvenido a Sabor del Himalaya!\n\n"
                    + "Por favor, active su cuenta haciendo clic en el siguiente enlace:\n"
                    + activationLink + "\n\n"
                    + "Este enlace expirará en 24 horas.\n"
                    + "Si no solicitó este registro, por favor ignore este mensaje.");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("No se pudo conectar con el servidor SMTP para enviar el correo a {}: {}. "
                    + "El enlace de activación sigue siendo válido en los logs para desarrollo local.",
                    toEmail, e.getMessage());
        }
    }
}
