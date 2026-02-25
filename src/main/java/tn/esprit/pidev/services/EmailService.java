package tn.esprit.pidev.services;

import tn.esprit.pidev.entities.Rental;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * EmailService - Sends rental notifications using JavaMail API.
 *
 * Sends:
 *  - Rental confirmation to renter
 *  - Rental approval notification
 *  - Overdue reminder
 *  - Rental completion summary
 *
 * Uses Gmail SMTP (configure your own credentials below).
 *
 * Dependency already in pom.xml:
 *   com.sun.mail:javax.mail:1.6.2
 *
 * MODULE: Rental - API #1
 */
public class EmailService {

    // ⚠️ Replace with your Gmail address and App Password
    // To get an App Password: Google Account > Security > 2-Step Verification > App Passwords
    private static final String SENDER_EMAIL = "yassinearfaoui689@gmail.com";
    private static final String SENDER_PASSWORD = "xxxx xxxx xxxx xxxx";
    private static final String SENDER_NAME = "AgriRent System";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Sends a rental CONFIRMATION email to the renter.
     *
     * @param rental the rental object (must have renterContact set to a valid email)
     * @return true if sent successfully
     */
    public boolean sendRentalConfirmation(Rental rental) {
        String subject = "✅ Rental Confirmed - AgriRent #" + rental.getRentalId();
        String body = buildConfirmationEmail(rental);
        return sendEmail(rental.getRenterContact(), subject, body);
    }

    /**
     * Sends an OVERDUE reminder email.
     *
     * @param rental the overdue rental
     * @return true if sent successfully
     */
    public boolean sendOverdueReminder(Rental rental) {
        String subject = "⚠️ Rental Overdue Notice - AgriRent #" + rental.getRentalId();
        String body = buildOverdueEmail(rental);
        return sendEmail(rental.getRenterContact(), subject, body);
    }

    /**
     * Sends a rental COMPLETION summary.
     *
     * @param rental the completed rental
     * @return true if sent successfully
     */
    public boolean sendCompletionSummary(Rental rental) {
        String subject = "📋 Rental Completed - AgriRent #" + rental.getRentalId();
        String body = buildCompletionEmail(rental);
        return sendEmail(rental.getRenterContact(), subject, body);
    }

    /**
     * Core email sending method using JavaMail SMTP.
     */
    public boolean sendEmail(String toEmail, String subject, String htmlBody) {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            System.err.println("❌ Invalid recipient email: " + toEmail);
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    //  Email Template Builders
    // ============================================================

    private String buildConfirmationEmail(Rental rental) {
        String equipName = rental.getInventory() != null ? rental.getInventory().getItemName() : "Equipment #" + rental.getInventoryId();
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;'>" +
               "<div style='background:linear-gradient(135deg,#2E7D32,#66BB6A);padding:30px;text-align:center;'>" +
               "<h1 style='color:white;margin:0;font-size:24px;'>🌿 AgriRent</h1>" +
               "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;'>Agricultural Equipment Rental</p>" +
               "</div>" +
               "<div style='padding:32px;background:#ffffff;'>" +
               "<h2 style='color:#2E7D32;'>Rental Confirmation ✅</h2>" +
               "<p>Dear <strong>" + rental.getRenterName() + "</strong>,</p>" +
               "<p>Your rental has been confirmed. Here are the details:</p>" +
               "<table style='width:100%;border-collapse:collapse;margin:20px 0;'>" +
               tableRow("Rental ID", "#" + rental.getRentalId()) +
               tableRow("Equipment", equipName) +
               tableRow("Start Date", rental.getStartDate() != null ? rental.getStartDate().format(DATE_FMT) : "N/A") +
               tableRow("End Date", rental.getEndDate() != null ? rental.getEndDate().format(DATE_FMT) : "N/A") +
               tableRow("Duration", rental.getTotalDays() + " days") +
               tableRow("Daily Rate", String.format("%.2f TND/day", rental.getDailyRate())) +
               tableRow("Total Cost", String.format("<strong style='color:#2E7D32;'>%.2f TND</strong>", rental.getTotalCost())) +
               tableRow("Delivery", rental.isRequiresDelivery() ? "Yes - " + rental.getDeliveryAddress() : "No (Self-pickup)") +
               "</table>" +
               "<div style='background:#F1F8E9;border-left:4px solid #2E7D32;padding:16px;border-radius:4px;'>" +
               "<p style='margin:0;color:#1B5E20;'><strong>📞 Owner Contact:</strong> " + rental.getOwnerName() + "</p>" +
               "</div>" +
               "<p style='margin-top:24px;color:#757575;font-size:13px;'>Thank you for using AgriRent. Please ensure to return the equipment in good condition.</p>" +
               "</div>" +
               "<div style='background:#F5F5F5;padding:16px;text-align:center;'>" +
               "<p style='margin:0;color:#9E9E9E;font-size:12px;'>AgriRent - Connecting Farmers | This is an automated message</p>" +
               "</div>" +
               "</div>";
    }

    private String buildOverdueEmail(Rental rental) {
        long overdueDays = rental.isOverdue() ? Math.abs(rental.getDaysUntilReturn()) : 0;
        double lateFee = rental.calculateLateFee();
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;'>" +
               "<div style='background:#D32F2F;padding:30px;text-align:center;'>" +
               "<h1 style='color:white;margin:0;'>⚠️ Overdue Notice</h1>" +
               "</div>" +
               "<div style='padding:32px;'>" +
               "<p>Dear <strong>" + rental.getRenterName() + "</strong>,</p>" +
               "<p>Your rental <strong>#" + rental.getRentalId() + "</strong> was due on <strong>" +
               (rental.getEndDate() != null ? rental.getEndDate().format(DATE_FMT) : "N/A") +
               "</strong> and is now <strong style='color:#D32F2F;'>" + overdueDays + " day(s) overdue</strong>.</p>" +
               "<p>Late fee accrued: <strong style='color:#D32F2F;'>" + String.format("%.2f TND", lateFee) + "</strong></p>" +
               "<p>Please return the equipment as soon as possible to avoid additional charges.</p>" +
               "</div>" +
               "</div>";
    }

    private String buildCompletionEmail(Rental rental) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;'>" +
               "<div style='background:linear-gradient(135deg,#2E7D32,#66BB6A);padding:30px;text-align:center;'>" +
               "<h1 style='color:white;margin:0;'>🎉 Rental Completed!</h1>" +
               "</div>" +
               "<div style='padding:32px;'>" +
               "<p>Dear <strong>" + rental.getRenterName() + "</strong>,</p>" +
               "<p>Your rental <strong>#" + rental.getRentalId() + "</strong> has been completed successfully.</p>" +
               "<p>Total charged: <strong style='color:#2E7D32;'>" + String.format("%.2f TND", rental.getTotalCost()) + "</strong></p>" +
               "<p>Thank you for using AgriRent! We hope to serve you again.</p>" +
               "</div>" +
               "</div>";
    }

    private String tableRow(String label, String value) {
        return "<tr style='border-bottom:1px solid #F5F5F5;'>" +
               "<td style='padding:10px;color:#757575;width:40%;'>" + label + "</td>" +
               "<td style='padding:10px;color:#2C2C2C;font-weight:500;'>" + value + "</td>" +
               "</tr>";
    }
}
