package tn.esprit.utils;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class VerificationCodeManager {
    private static VerificationCodeManager instance;
    private final Map<String, VerificationEntry> verificationCodes;
    private final Map<String, VerificationEntry> passwordResetCodes;

    private VerificationCodeManager() {
        this.verificationCodes = new ConcurrentHashMap<>();
        this.passwordResetCodes = new ConcurrentHashMap<>();

        // Start cleanup thread to remove expired codes
        startCleanupThread();
    }

    public static synchronized VerificationCodeManager getInstance() {
        if (instance == null) {
            instance = new VerificationCodeManager();
        }
        return instance;
    }

    /**
     * Store email verification code
     */
    public void storeVerificationCode(String email, String code) {
        VerificationEntry entry = new VerificationEntry(code, LocalDateTime.now().plusMinutes(10));
        verificationCodes.put(email.toLowerCase(), entry);
        System.out.println("Stored verification code for: " + email);
    }

    /**
     * Store password reset code
     */
    public void storePasswordResetCode(String email, String code) {
        VerificationEntry entry = new VerificationEntry(code, LocalDateTime.now().plusMinutes(15));
        passwordResetCodes.put(email.toLowerCase(), entry);
        System.out.println("Stored password reset code for: " + email);
    }

    /**
     * Verify email verification code
     */
    public boolean verifyEmailCode(String email, String code) {
        VerificationEntry entry = verificationCodes.get(email.toLowerCase());
        if (entry == null) {
            System.out.println("No verification code found for: " + email);
            return false;
        }

        if (entry.isExpired()) {
            verificationCodes.remove(email.toLowerCase());
            System.out.println("Verification code expired for: " + email);
            return false;
        }

        boolean isValid = entry.getCode().equals(code);
        if (isValid) {
            verificationCodes.remove(email.toLowerCase()); // Remove used code
            System.out.println("Email verification successful for: " + email);
        } else {
            System.out.println("Invalid verification code for: " + email);
        }

        return isValid;
    }

    /**
     * Verify password reset code
     */
    public boolean verifyPasswordResetCode(String email, String code) {
        VerificationEntry entry = passwordResetCodes.get(email.toLowerCase());
        if (entry == null) {
            System.out.println("No password reset code found for: " + email);
            return false;
        }

        if (entry.isExpired()) {
            passwordResetCodes.remove(email.toLowerCase());
            System.out.println("Password reset code expired for: " + email);
            return false;
        }

        boolean isValid = entry.getCode().equals(code);
        if (isValid) {
            passwordResetCodes.remove(email.toLowerCase()); // Remove used code
            System.out.println("Password reset code verification successful for: " + email);
        } else {
            System.out.println("Invalid password reset code for: " + email);
        }

        return isValid;
    }

    /**
     * Check if verification code exists for email
     */
    public boolean hasValidVerificationCode(String email) {
        VerificationEntry entry = verificationCodes.get(email.toLowerCase());
        return entry != null && !entry.isExpired();
    }

    /**
     * Check if password reset code exists for email
     */
    public boolean hasValidPasswordResetCode(String email) {
        VerificationEntry entry = passwordResetCodes.get(email.toLowerCase());
        return entry != null && !entry.isExpired();
    }

    /**
     * Clear verification code for email
     */
    public void clearVerificationCode(String email) {
        verificationCodes.remove(email.toLowerCase());
    }

    /**
     * Clear password reset code for email
     */
    public void clearPasswordResetCode(String email) {
        passwordResetCodes.remove(email.toLowerCase());
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Run every minute
                    cleanupExpiredCodes();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();

        // Clean up verification codes
        verificationCodes.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Clean up password reset codes
        passwordResetCodes.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class VerificationEntry {
        private final String code;
        private final LocalDateTime expiryTime;

        public VerificationEntry(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }

        public String getCode() {
            return code;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}
