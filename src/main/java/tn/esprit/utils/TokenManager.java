package tn.esprit.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.prefs.Preferences;

public class TokenManager {
    private static final String SECRET_KEY = "DigitalFarm2026!"; // 16 chars for AES
    private static final String ALGORITHM = "AES";
    private static final Preferences prefs = Preferences.userNodeForPackage(TokenManager.class);

    /**
     * Save encrypted user ID token
     */
    public static void saveToken(int userId) {
        try {
            String token = encrypt(String.valueOf(userId));
            prefs.put("authToken", token);
            prefs.putLong("tokenExpiry", System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)); // 7 days
            System.out.println("✅ Token saved for user ID: " + userId);
        } catch (Exception e) {
            System.err.println("Failed to save token: " + e.getMessage());
        }
    }

    /**
     * Get decrypted user ID from token
     * Returns -1 if no valid token exists
     */
    public static int getUserIdFromToken() {
        try {
            String token = prefs.get("authToken", null);
            long expiry = prefs.getLong("tokenExpiry", 0);

            // Check if token exists and is not expired
            if (token == null || System.currentTimeMillis() > expiry) {
                System.out.println("❌ Token expired or doesn't exist");
                clearToken();
                return -1;
            }

            String userIdStr = decrypt(token);
            int userId = Integer.parseInt(userIdStr);
            System.out.println("✅ Valid token found for user ID: " + userId);
            return userId;
        } catch (Exception e) {
            System.err.println("Failed to read token: " + e.getMessage());
            clearToken();
            return -1;
        }
    }

    /**
     * Clear saved token (on logout)
     */
    public static void clearToken() {
        prefs.remove("authToken");
        prefs.remove("tokenExpiry");
        System.out.println("🗑️ Token cleared");
    }

    /**
     * Check if valid token exists
     */
    public static boolean hasValidToken() {
        return getUserIdFromToken() != -1;
    }

    // AES Encryption
    private static String encrypt(String value) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // AES Decryption
    private static String decrypt(String encrypted) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
