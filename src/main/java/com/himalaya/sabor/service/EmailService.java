package com.himalaya.sabor.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendActivationEmail(String recipientName, String toEmail, String activationLink) {
        log.info("================================================================================");
        log.info("📧 PREPARANDO ENVÍO DE CORREO (SMTP GMAIL + THYMELEAF)");
        log.info("Destinatario: {} <{}>", recipientName, toEmail);
        log.info("👉 Enlace de activación: {}", activationLink);
        log.info("================================================================================");

        try {
            // 1. Preparar el contexto de Thymeleaf
            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("activationUrl", activationLink);

            // 2. Procesar la plantilla HTML usando SpringTemplateEngine
            String htmlContent = templateEngine.process("mail/activation-email", context);

            // 3. Construir el MimeMessage
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String senderAddress = (fromEmail != null && !fromEmail.isBlank())
                    ? fromEmail
                    : "no-reply@sabordelhimalaya.com";

            helper.setFrom(new InternetAddress(senderAddress, "Sabor del Himalaya"));
            helper.setTo(toEmail);
            helper.setSubject("Activa tu cuenta - Sabor del Himalaya");
            helper.setText(htmlContent, true);

            // 4. Enviar vía SMTP de Gmail
            mailSender.send(mimeMessage);
            log.info("✓ [ÉXITO] Correo HTML enviado satisfactoriamente a {}", toEmail);

        } catch (Exception e) {
            log.error("⚠️ [ERROR AL ENVIAR CORREO GMAIL] {}: {}. "
                    + "Verifique las variables SPRING_MAIL_USERNAME y SPRING_MAIL_PASSWORD (App Password de Gmail de 16 caracteres). "
                    + "Enlace disponible para pruebas: {}",
                    e.getClass().getSimpleName(), e.getMessage(), activationLink);
        }
    }

    public void sendOrderCancellationEmail(com.himalaya.sabor.model.User user, com.himalaya.sabor.model.Order order) {
        log.info("📧 ENVIANDO NOTIFICACIÓN DE CANCELACIÓN DE PEDIDO {} a {}", order.getOrderNumber(), user.getEmail());
        try {
            Context context = new Context();
            context.setVariable("recipientName", user.getName());
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("totalAmount", order.getTotalAmount());

            String htmlContent = templateEngine.process("mail/order-cancelled-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String senderAddress = (fromEmail != null && !fromEmail.isBlank())
                    ? fromEmail
                    : "no-reply@sabordelhimalaya.com";

            helper.setFrom(new InternetAddress(senderAddress, "Sabor del Himalaya"));
            helper.setTo(user.getEmail());
            helper.setSubject("Pedido cancelado sin coste (" + order.getOrderNumber() + ") - Sabor del Himalaya");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✓ [ÉXITO] Notificación de cancelación enviada a {}", user.getEmail());
        } catch (Exception e) {
            log.error("⚠️ [ERROR AL ENVIAR NOTIFICACIÓN DE CANCELACIÓN] {}: {}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
