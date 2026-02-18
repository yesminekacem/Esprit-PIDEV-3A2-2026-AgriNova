package tn.esprit.utils;

import java.util.regex.Pattern;

public class ValidationUtil {

    // Email regex pattern (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Password must contain at least one lowercase, uppercase, digit, and special character
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    /**
     * Validate email format
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate password strength
     * @param password The password to validate
     * @return true if password meets all requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        return LOWERCASE_PATTERN.matcher(password).matches() &&
                UPPERCASE_PATTERN.matcher(password).matches() &&
                DIGIT_PATTERN.matcher(password).matches() &&
                SPECIAL_CHAR_PATTERN.matcher(password).matches();
    }

    /**
     * Get detailed password validation message
     * @param password The password to check
     * @return Error message describing what's missing, or empty string if valid
     */
    public static String getPasswordValidationMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one lowercase letter (a-z)";
        }

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one uppercase letter (A-Z)";
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one digit (0-9)";
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one special character (!@#$%^&*...)";
        }

        return ""; // Valid
    }

    /**
     * Get detailed email validation message
     * @param email The email to check
     * @return Error message or empty string if valid
     */
    public static String getEmailValidationMessage(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required";
        }

        if (!isValidEmail(email)) {
            return "Invalid email format (example: user@domain.com)";
        }

        return ""; // Valid
    }

    /**
     * Validate full name (not empty, at least 2 characters)
     * @param fullName The name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidFullName(String fullName) {
        return fullName != null && fullName.trim().length() >= 2;
    }

    /**
     * Get full name validation message
     * @param fullName The name to check
     * @return Error message or empty string if valid
     */
    public static String getFullNameValidationMessage(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Full name is required";
        }

        if (fullName.trim().length() < 2) {
            return "Full name must be at least 2 characters";
        }

        return ""; // Valid
    }
}
