package tn.esprit.marketplace.service;

import tn.esprit.utils.PayPalConfig;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * PayPal Service for Sandbox checkout
 * Uses Java 11+ HttpClient (no external HTTP library needed)
 */
public class PayPalService {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Get PayPal access token using OAuth2 Client Credentials flow
     * POST to /v1/oauth2/token with Basic Auth
     */
    public String getAccessToken() throws Exception {
        // Create Basic Auth header: base64(clientId:clientSecret)
        String auth = PayPalConfig.CLIENT_ID + ":" + PayPalConfig.CLIENT_SECRET;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        // Request body: grant_type=client_credentials
        String requestBody = "grant_type=client_credentials";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(PayPalConfig.API_BASE + "/v1/oauth2/token"))
                .header("Authorization", "Basic " + encodedAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[PayPal] Token Request - Status: " + response.statusCode());
        System.out.println("[PayPal] Token Response: " + response.body());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to get PayPal access token. Status: " + response.statusCode()
                    + ", Response: " + response.body());
        }

        // Parse JSON manually to extract access_token
        // Response format: {"scope":"...","access_token":"...","app_id":"...","expires_in":3600,"token_type":"Bearer"}
        String token = extractJsonValue(response.body(), "\"access_token\":\"", "\"");
        if (token == null || token.isEmpty()) {
            throw new Exception("No access_token in response: " + response.body());
        }

        System.out.println("[PayPal] Token obtained successfully");
        return token;
    }

    /**
     * Create a PayPal order
     * POST to /v2/checkout/orders
     */
    public CreateOrderResult createOrder(String accessToken, String currencyCode, double amount) throws Exception {
        // Validate amount
        if (amount <= 0) {
            throw new Exception("Invalid amount: " + amount + ". Amount must be greater than 0");
        }
        if (amount < 0.01) {
            throw new Exception("Invalid amount: " + amount + ". Minimum amount is 0.01");
        }

        // Build JSON payload
        String jsonPayload = buildCreateOrderJson(currencyCode, amount);

        System.out.println("[PayPal] Creating order with amount: " + amount + " " + currencyCode);
        System.out.println("[PayPal] Request body: " + jsonPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(PayPalConfig.API_BASE + "/v2/checkout/orders"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[PayPal] Order Response Status: " + response.statusCode());
        System.out.println("[PayPal] Order Response: " + response.body());

        if (response.statusCode() != 201) {
            throw new Exception("Failed to create PayPal order. Status: " + response.statusCode()
                    + ", Response: " + response.body());
        }

        // Parse response to extract order ID and approve link
        String orderId = extractJsonValue(response.body(), "\"id\":\"", "\"");
        if (orderId == null || orderId.isEmpty()) {
            throw new Exception("No order ID in response: " + response.body());
        }

        // Extract approve URL from links array
        // Look for: "rel":"approve","href":"https://..."
        String approveUrl = extractApproveUrl(response.body());
        if (approveUrl == null || approveUrl.isEmpty()) {
            throw new Exception("No approve URL found in response: " + response.body());
        }

        System.out.println("[PayPal] Order created successfully. ID: " + orderId);
        return new CreateOrderResult(orderId, approveUrl);
    }

    /**
     * Capture (finalize) a PayPal order after user approval
     * POST to /v2/checkout/orders/{id}/capture
     */
    public String captureOrder(String accessToken, String orderId) throws Exception {
        System.out.println("[PayPal] Capturing order: " + orderId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(PayPalConfig.API_BASE + "/v2/checkout/orders/" + orderId + "/capture"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[PayPal] Capture Response Status: " + response.statusCode());
        System.out.println("[PayPal] Capture Response: " + response.body());

        if (response.statusCode() != 201) {
            throw new Exception("Failed to capture PayPal order. Status: " + response.statusCode()
                    + ", Response: " + response.body());
        }

        // Parse response to extract status
        String status = extractJsonValue(response.body(), "\"status\":\"", "\"");
        if (status == null || status.isEmpty()) {
            throw new Exception("No status in capture response: " + response.body());
        }

        System.out.println("[PayPal] Capture successful. Status: " + status);
        return status;
    }

    /**
     * Helper: Build JSON for creating an order
     */
    private String buildCreateOrderJson(String currencyCode, double amount) {
        return "{\n" +
                "  \"intent\": \"CAPTURE\",\n" +
                "  \"purchase_units\": [\n" +
                "    {\n" +
                "      \"amount\": {\n" +
                "        \"currency_code\": \"" + currencyCode + "\",\n" +
                "        \"value\": \"" + String.format("%.2f", amount) + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * Helper: Extract value from simple JSON responses (no external JSON library)
     * Finds: prefix"value"suffix and extracts what's between the quotes after prefix
     */
    private String extractJsonValue(String json, String prefix, String suffix) {
        int startIdx = json.indexOf(prefix);
        if (startIdx == -1) return null;

        int valueStart = startIdx + prefix.length();
        int valueEnd = json.indexOf(suffix, valueStart);

        if (valueEnd == -1) return null;

        return json.substring(valueStart, valueEnd);
    }

    /**
     * Helper: Extract approve URL from links array
     * Looking for the link with "rel":"approve"
     */
    private String extractApproveUrl(String json) {
        // Find all occurrences of "rel":"approve"
        String searchPattern = "\"rel\":\"approve\"";
        int approveIdx = json.indexOf(searchPattern);

        if (approveIdx == -1) {
            System.out.println("[PayPal] No approve link found in response");
            return null;
        }

        // Search backwards from approveIdx to find the opening of this link object {
        int linkStart = json.lastIndexOf("{", approveIdx);
        if (linkStart == -1) {
            System.out.println("[PayPal] Could not find link object start");
            return null;
        }

        // Search forwards from linkStart to find "href"
        int hrefIdx = json.indexOf("\"href\"", linkStart);
        if (hrefIdx == -1 || hrefIdx > approveIdx + 20) {
            // href should be near approve link
            System.out.println("[PayPal] Could not find href near approve link");
            return null;
        }

        // Extract the URL between quotes after "href":"
        int urlStart = json.indexOf("\"", hrefIdx + 6) + 1; // Skip past "href":"
        int urlEnd = json.indexOf("\"", urlStart);

        if (urlStart <= 0 || urlEnd == -1) {
            System.out.println("[PayPal] Could not extract URL boundaries");
            return null;
        }

        String approveUrl = json.substring(urlStart, urlEnd);
        System.out.println("[PayPal] Extracted approve URL: " + approveUrl);
        return approveUrl;
    }

    /**
     * Result class for order creation
     */
    public static class CreateOrderResult {
        public final String orderId;
        public final String approveUrl;

        public CreateOrderResult(String orderId, String approveUrl) {
            this.orderId = orderId;
            this.approveUrl = approveUrl;
        }
    }
}

