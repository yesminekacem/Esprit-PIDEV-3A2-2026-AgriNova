package tn.esprit.utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class EmailService {
    // SMTP Configuration - Using Gmail SMTP (you can change to any SMTP server)
    private static final String SMTP_HOST = "smtp.mailersend.net";
    private static final String SMTP_PORT = "587";
    private static final String FROM_EMAIL = "MS_Lpf762@test-51ndgwvmwk5lzqx8.mlsender.net"; // Replace with your email
    private static final String EMAIL_PASSWORD = "mssp.4YNiRBE.7dnvo4dpder45r86.X4t6bq6"; // Replace with your app password
    private static final String FROM_NAME = "Agrinova Platform";

    private final Properties properties;
    public EmailService() {
        // Configure SMTP properties with optimization
        properties = new Properties();
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Add timeout settings for faster response
        properties.put("mail.smtp.connectiontimeout", "5000"); // 5 second connection timeout
        properties.put("mail.smtp.timeout", "10000"); // 10 second read timeout
        properties.put("mail.smtp.writetimeout", "5000"); // 5 second write timeout
    }

    /**
     * Generate a 6-digit verification code
     */
    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Generates 6-digit number
        return String.valueOf(code);
    }

    /**
     * Send email verification code asynchronously
     */
    public boolean sendVerificationEmail(String recipientEmail, String recipientName, String verificationCode) {
        // For immediate UI response, return true and send email in background
        sendEmailAsync(recipientEmail, recipientName, "Verify Your Email - Agrinova Platform",
                      buildVerificationEmailHtml(recipientName, verificationCode, recipientEmail), verificationCode);
        return true;
    }

    /**
     * Send password reset email asynchronously
     */
    public boolean sendPasswordResetEmail(String recipientEmail, String recipientName, String resetCode) {
        // For immediate UI response, return true and send email in background
        sendEmailAsync(recipientEmail, recipientName, "Reset Your Password - Agrinova Platform",
                      buildPasswordResetEmailHtml(recipientName, resetCode), resetCode);
        return true;
    }

    /**
     * Send email asynchronously to prevent UI blocking
     */
    private void sendEmailAsync(String recipientEmail, String recipientName, String subject, String htmlContent, String code) {
        CompletableFuture.runAsync(() -> {
            try {
                Session session = createSession();

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail, recipientName));
                message.setSubject(subject);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                System.out.println("✅ Email sent successfully to: " + recipientEmail);

            } catch (Exception e) {
                System.err.println("❌ Failed to send email: " + e.getMessage());
                // Always show the code in development mode for testing
                System.out.println("⚠️ EMAIL SIMULATION MODE - Code: " + code);
            }
        });
    }

    private Session createSession() {
        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
            }
        });
    }

    private String buildVerificationEmailHtml(String recipientName, String verificationCode, String recipientEmail) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Email Verification - Agrinova Platform</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #2E7D32; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h1 style="margin: 0;">🌱 Agrinova Platform</h1>
                    <p style="margin: 5px 0 0 0;">Agricultural Management System</p>
                </div>
                
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #2E7D32;">Welcome to Agrinova, %s!</h2>
                    
                    <p>Thank you for signing up with Agrinova Platform. To complete your registration and verify your email address, please use the verification code below:</p>
                    
                    <div style="background-color: white; border: 2px dashed #2E7D32; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px;">
                        <h3 style="margin: 0; font-size: 32px; font-weight: bold; color: #2E7D32; letter-spacing: 3px;">%s</h3>
                    </div>
                    
                    <p><strong>Important:</strong></p>
                    <ul>
                        <li>This verification code will expire in 10 minutes</li>
                        <li>Enter this code in the signup form to verify your email</li>
                        <li>If you didn't request this verification, please ignore this email</li>
                    </ul>
                    
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    
                    <p style="color: #666; font-size: 14px;">
                        This email was sent to %s. If you have any questions or need assistance, please contact our support team.
                    </p>
                    
                    <p style="color: #666; font-size: 12px; text-align: center; margin-top: 20px;">
                        © 2026 Agrinova Platform. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(recipientName, verificationCode, recipientEmail);
    }

    private String buildPasswordResetEmailHtml(String recipientName, String resetCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Reset - Agrinova Platform</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #d32f2f; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h1 style="margin: 0;">🔒 Password Reset Request</h1>
                    <p style="margin: 5px 0 0 0;">Agrinova Platform</p>
                </div>
                
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #d32f2f;">Password Reset for %s</h2>
                    
                    <p>We received a request to reset your password for your Agrinova Platform account. Use the reset code below to create a new password:</p>
                    
                    <div style="background-color: white; border: 2px dashed #d32f2f; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px;">
                        <h3 style="margin: 0; font-size: 32px; font-weight: bold; color: #d32f2f; letter-spacing: 3px;">%s</h3>
                    </div>
                    
                    <p><strong>Important:</strong></p>
                    <ul>
                        <li>This reset code will expire in 15 minutes</li>
                        <li>Enter this code in the password reset form</li>
                        <li>If you didn't request this reset, please ignore this email and ensure your account is secure</li>
                    </ul>
                    
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    
                    <p style="color: #666; font-size: 14px;">
                        This email was sent to your registered email address. If you have any questions or need assistance, please contact our support team.
                    </p>
                    
                    <p style="color: #666; font-size: 12px; text-align: center; margin-top: 20px;">
                        © 2026 Agrinova Platform. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(recipientName, resetCode);
    }
}
