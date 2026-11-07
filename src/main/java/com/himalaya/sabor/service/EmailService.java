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
        log.info("================================================================================");
        log.info("✉️  [ENTORNO DE DESARROLLO] CORREO DE ACTIVACIÓN GENERADO");
        log.info("Para: {}", toEmail);
        log.info("👉 Enlace de activación: {}", activationLink);
        log.info("================================================================================");

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
            log.info("✓ Correo enviado exitosamente vía SMTP local.");
        } catch (Exception e) {
            log.info("ℹ️ Servidor SMTP local no disponible en localhost:1025. "
                    + "Puede usar directamente el enlace impreso arriba o levantar Mailpit con 'docker compose up -d'.");
        }
    }
}
