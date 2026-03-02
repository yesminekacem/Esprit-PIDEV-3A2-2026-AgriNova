package tn.esprit.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Face ID service — uses Face++ free API to compare two face images.
 * Free tier: 1000 calls/month, no credit card needed.
 * Sign up at: https://www.faceplusplus.com/
 * Then replace the two constants below with your credentials.
 */
public class FaceIdService {

    // ── ▶  PASTE YOUR CREDENTIALS HERE ──────────────────────────────
    public static final String API_KEY    = "6USQGDgxXxxzyN8dkKtcLMR79IAhBDpj";
    public static final String API_SECRET = "5wkSILEtjCXbyHsNp-21w8B6b_RfvGu0";
    // ────────────────────────────────────────────────────────────────

    private static final String COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";

    /** Minimum confidence score (0–100) required to accept a match */
    public static final double THRESHOLD = 76.0;

    // ── Image helpers ─────────────────────────────────────────────────

    public static String toBase64(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public static BufferedImage fromBase64(String b64) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(b64)));
    }

    // ── API ───────────────────────────────────────────────────────────

    /**
     * Compare two Base64-encoded face images via Face++ /compare.
     * @return confidence 0-100, or -1 on error / no face detected
     */
    public static double compare(String b64a, String b64b) {
        try {
            String boundary = "FaceIdBnd" + System.currentTimeMillis();
            byte[] body = multipart(boundary,
                    "api_key",        API_KEY,
                    "api_secret",     API_SECRET,
                    "image_base64_1", b64a,
                    "image_base64_2", b64b);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(COMPARE_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            String json = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString())
                    .body();

            System.out.println("Face++ response: " + json);
            return parseConfidence(json);
        } catch (Exception e) {
            System.err.println("Face++ error: " + e.getMessage());
            return -1;
        }
    }

    /** Returns true if the two images are the same person */
    public static boolean verify(String storedB64, BufferedImage live) {
        try {
            double score = compare(storedB64, toBase64(live));
            System.out.printf("Face confidence: %.1f%% (need %.1f%%)%n", score, THRESHOLD);
            return score >= THRESHOLD;
        } catch (IOException e) {
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static double parseConfidence(String json) {
        if (json == null) return -1;
        int i = json.indexOf("\"confidence\"");
        if (i < 0) return -1;
        int colon = json.indexOf(':', i);
        int comma = json.indexOf(',', colon);
        int brace = json.indexOf('}', colon);
        int end = (comma > 0 && (brace < 0 || comma < brace)) ? comma : brace;
        if (colon < 0 || end < 0) return -1;
        try {
            return Double.parseDouble(json.substring(colon + 1, end).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static byte[] multipart(String boundary, String... kv) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String CRLF = "\r\n";
        for (int i = 0; i < kv.length; i += 2) {
            out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"" + kv[i] + "\"" + CRLF)
                    .getBytes(StandardCharsets.UTF_8));
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));
            out.write(kv[i + 1].getBytes(StandardCharsets.UTF_8));
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}

