package tn.esprit.utils;

public class PayPalConfig {

    public static final String CLIENT_ID = "AU4GL1ECCDoOxKUS_uiP6cS1DknsQtexNUlqxcd-eT-IhWbx4JUBZ_db0Po3gpmco3KbF2mU7aH7GYzu";
    public static final String CLIENT_SECRET = "EFb7t6VkR-Ay98xpH9LibHUMxq7CQQp58US1iXwLhSODLktaii2TdY15P0xNRkQtEeIBn5BblG1bmPoU";

    public static final String API_BASE = "https://api-m.sandbox.paypal.com";

    public static final String RETURN_URL = "http://localhost/return";
    public static final String CANCEL_URL = "http://localhost/cancel";

    public static final String PAYPAL_CURRENCY = "USD";

    public static final double TND_TO_USD_RATE = 0.32; // Approximate: 1 TND ≈ 0.32 USD


    public static double convertTNDtoUSD(double tndAmount) {
        return Math.round(tndAmount * TND_TO_USD_RATE * 100.0) / 100.0;
    }
}

