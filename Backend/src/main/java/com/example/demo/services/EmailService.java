package com.example.demo.services;

import com.example.demo.entities.Invoice;
import com.example.demo.entities.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    public void sendApprovalEmail(User user) {

        // 🔥 For now just log (replace later with Twilio)
        System.out.println("Sending email to: " + user.getEmail());

        System.out.println("""
        ==========================
        ACCOUNT APPROVED
        ==========================
        Email: %s
        Password: (user already set)
        You can now login.
        ==========================
        """.formatted(user.getEmail()));

    }


        public void sendInvoice(Invoice invoice) {

            MimeMessage message = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setTo(invoice.getProject().getOwner().getEmail());
                helper.setSubject("Your invoice " + invoice.getInvoiceNumber());
                helper.setText("Please find your invoice for " +
                        invoice.getBillingPeriod() + " attached.");
                helper.addAttachment(
                        invoice.getInvoiceNumber() + ".pdf",
                        new File(invoice.getPdfPath())
                );
                mailSender.send(message);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send invoice email", e);
            }
        }
    }
