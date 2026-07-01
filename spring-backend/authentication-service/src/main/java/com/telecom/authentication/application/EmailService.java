package com.telecom.authentication.application;

import com.telecom.authentication.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@telecom-billing.tn}")
    private String fromEmail;

    @Value("${app.gateway-url:http://localhost:8080}")
    private String gatewayUrl;

    public void sendSetPasswordEmail(User user, String token) {
        String setPasswordLink = gatewayUrl + "/auth/set-password?token=" + token;
        
        log.info("\n==================================================\n" +
                 "CREATE USER EMAIL DEMO SIMULATION:\n" +
                 "To: {}\n" +
                 "Set Password Link: {}\n" +
                 "==================================================", user.getEmail(), setPasswordLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "TélécomBilling System");
            helper.setTo(user.getEmail());
            helper.setSubject("Initialisation de votre compte TélécomBilling");

            String htmlBody = String.format(
                "<h3>Bienvenue chez TélécomBilling, %s %s !</h3>" +
                "<p>Votre compte utilisateur a été créé avec succès par l'administrateur.</p>" +
                "<p>Veuillez cliquer sur le lien ci-dessous pour choisir votre mot de passe et activer votre compte :</p>" +
                "<p><a href='%s' style='display:inline-block;background:#E28743;color:white;padding:10px 20px;text-decoration:none;border-radius:4px;'>Définir mon mot de passe</a></p>" +
                "<p>Ce lien est valide pour 24 heures.</p>" +
                "<br/>" +
                "<p>Cordialement,<br/>L'équipe TélécomBilling</p>",
                user.getFirstName(), user.getLastName(), setPasswordLink
            );

            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Set-password email successfully sent to {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Could not email link to {} (SMTP fallback): {}", user.getEmail(), e.getMessage());
        }
    }
}
