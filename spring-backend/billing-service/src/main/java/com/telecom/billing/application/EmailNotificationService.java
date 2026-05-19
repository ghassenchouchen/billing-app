package com.telecom.billing.application;

import com.telecom.billing.domain.entity.Facture;
import com.telecom.billing.infrastructure.client.CustomerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final InvoicePdfService pdfService;
    private final CustomerClient customerClient;

    @Value("${spring.mail.username:noreply@telecom-billing.tn}")
    private String fromEmail;

    @Value("${billing.company-name:TélécomBilling}")
    private String companyName;

    /**
     * Send invoice PDF as an email attachment to the customer.
     * Called when an invoice transitions to SENT status.
     */
    public void sendInvoiceEmail(Facture facture) {
        String recipientEmail = null;
        String recipientName = "Client";

        try {
            CustomerClient.ClientDto client = customerClient.getCustomerById(facture.getClientId());
            recipientEmail = client.email();
            recipientName = client.fullName();
        } catch (Exception e) {
            log.warn("Could not resolve customer email for client {}: {}", facture.getClientId(), e.getMessage());
            return;
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("No email address found for client {} — skipping email delivery", facture.getClientId());
            return;
        }

        try {
            byte[] pdfBytes = pdfService.generatePdf(facture);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, companyName);
            helper.setTo(recipientEmail);
            helper.setSubject(String.format("Votre facture %s — %s", facture.getNumeroFacture(), companyName));

            String body = String.format(
                """
                Bonjour %s,

                Veuillez trouver ci-joint votre facture N° %s d'un montant de %.3f TND.

                Date d'émission : %s
                Date d'échéance : %s

                En cas de questions, n'hésitez pas à nous contacter.

                Cordialement,
                %s
                """,
                recipientName,
                facture.getNumeroFacture(),
                facture.getMontantTTC(),
                facture.getDateFacture(),
                facture.getDateEcheance(),
                companyName
            );

            helper.setText(body, false);

            DataSource pdfAttachment = new ByteArrayDataSource(pdfBytes, "application/pdf");
            helper.addAttachment("facture-" + facture.getNumeroFacture() + ".pdf", pdfAttachment);

            mailSender.send(message);
            log.info("Invoice email sent to {} for invoice {}", recipientEmail, facture.getNumeroFacture());

        } catch (Exception e) {
            log.error("Failed to send invoice email to {} for invoice {}: {}",
                recipientEmail, facture.getNumeroFacture(), e.getMessage(), e);
        }
    }
}
