package tn.esprit.pidev.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AIService — calls the AgriRent Flask AI API to get inventory predictions.
 *
 * The Flask API must be running on localhost:5000 before calling this class.
 * Start it with:  python api.py
 *
 * No external JSON library needed — we build/parse JSON manually
 * to avoid adding another pom.xml dependency.
 *
 * MODULE: Inventory — Advanced AI Feature
 */
public class AIService {

    private static final String BASE_URL = "http://127.0.0.1:5000";
    private static final int    TIMEOUT_MS = 5000;

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Result class holding all data returned by the API.
     */
    public static class AIResult {
        public int    predictedQuantity;
        public String maintenanceAdvice;
        public String rentalAdvice;
        public String confidence;
        public String errorMessage;

        public boolean isSuccess() { return errorMessage == null; }

        @Override
        public String toString() {
            if (!isSuccess()) return "Error: " + errorMessage;
            return String.format(
                    "Predicted Quantity: %d\nMaintenance: %s\nRental: %s\nConfidence: %s",
                    predictedQuantity, maintenanceAdvice, rentalAdvice, confidence);
        }
    }

    /**
     * Calls /predict and returns an AIResult.
     *
     * @param itemType            e.g. "TOOL", "EQUIPMENT", "CONSUMABLE", "STORAGE"
     * @param totalUsageHours     total hours the item has been used
     * @param conditionStatus     e.g. "GOOD", "EXCELLENT", "FAIR", "POOR"
     * @param isRentable          1 = rentable, 0 = not
     * @param rentalStatus        e.g. "AVAILABLE", "RENTED_OUT", "MAINTENANCE"
     * @param unitPrice           purchase/unit price in TND
     * @param rentalPricePerDay   daily rental rate in TND
     * @return AIResult with prediction and advice, or error message
     */
    public AIResult predict(String itemType, double totalUsageHours,
                            String conditionStatus, int isRentable,
                            String rentalStatus, double unitPrice,
                            double rentalPricePerDay) {
        try {
            String json = buildJson(
                    itemType, totalUsageHours, conditionStatus,
                    isRentable, rentalStatus, unitPrice, rentalPricePerDay
            );

            String response = post(BASE_URL + "/predict", json);
            return parseResult(response);

        } catch (Exception e) {
            AIResult result = new AIResult();
            result.errorMessage = e.getMessage();
            System.err.println("[AIService] Prediction failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Quick health check — returns true if the API is running.
     */
    public boolean isApiRunning() {
        try {
            String response = get(BASE_URL + "/health");
            return response.contains("ok");
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HTTP
    // ─────────────────────────────────────────────────────────────

    private String post(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("utf-8"));
        }

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  JSON BUILDER  (no external library needed)
    // ─────────────────────────────────────────────────────────────

    private String buildJson(String itemType, double totalUsageHours,
                             String conditionStatus, int isRentable,
                             String rentalStatus, double unitPrice,
                             double rentalPricePerDay) {
        return "{"
                + "\"item_type\":\"" + escapeJson(itemType) + "\","
                + "\"total_usage_hours\":" + totalUsageHours + ","
                + "\"condition_status\":\"" + escapeJson(conditionStatus) + "\","
                + "\"is_rentable\":" + isRentable + ","
                + "\"rental_status\":\"" + escapeJson(rentalStatus) + "\","
                + "\"unit_price\":" + unitPrice + ","
                + "\"rental_price_per_day\":" + rentalPricePerDay
                + "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ─────────────────────────────────────────────────────────────
    //  JSON PARSER  (simple manual parsing)
    // ─────────────────────────────────────────────────────────────

    private AIResult parseResult(String json) {
        AIResult result = new AIResult();

        if (json.contains("\"error\"")) {
            result.errorMessage = extractString(json, "error");
            return result;
        }

        result.predictedQuantity = (int) extractDouble(json, "predicted_quantity");
        result.maintenanceAdvice = extractString(json, "maintenance_advice");
        result.rentalAdvice      = extractString(json, "rental_advice");
        result.confidence        = extractString(json, "confidence");
        return result;
    }

    /** Extracts a string value: "key": "value" */
    private String extractString(String json, String key) {
        // Try with space after colon first: "key": "value"
        String search = "\"" + key + "\": \"";
        int start = json.indexOf(search);

        // Fallback: try without space: "key":"value"
        if (start == -1) {
            search = "\"" + key + "\":\"";
            start = json.indexOf(search);
        }

        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    /** Extracts a numeric value: "key": 42 */
    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0;
        start += search.length();
        // skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && "0123456789.-".indexOf(json.charAt(end)) >= 0) end++;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}