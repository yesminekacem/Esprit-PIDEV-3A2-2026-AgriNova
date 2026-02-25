package tn.esprit.pidev.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;

/**
 * WeatherService - Integrates OpenWeatherMap API
 * Used to suggest optimal maintenance windows for farm equipment.
 *
 * API: OpenWeatherMap (https://openweathermap.org/api)
 * Free tier: 1000 calls/day
 *
 * MODULE: Inventory - API #1
 */
public class WeatherService {

    // ⚠️ Replace with your own key from https://openweathermap.org/api
    private static final String API_KEY = "b12701d2f27aa920665eeae519c325af";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    /**
     * Fetches current weather for a given city.
     * Returns a WeatherInfo object with temperature, description, humidity, and wind.
     *
     * @param city e.g. "Tunis"
     * @return WeatherInfo or null on failure
     */
    public WeatherInfo getCurrentWeather(String city) {
        try {
            String urlStr = BASE_URL + "?q=" + city + "&appid=" + API_KEY + "&units=metric";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            System.out.println("API Response Code: " + responseCode);
            if (responseCode != 200) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder error = new StringBuilder();
                String errLine;
                while ((errLine = errorReader.readLine()) != null) {
                    error.append(errLine);
                }
                System.err.println("API Error Body: " + error.toString());
                // ... rest
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            return parseWeatherJson(sb.toString());

        } catch (Exception e) {
            System.err.println("❌ Weather API error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Simple JSON parser (no external library needed).
     * Extracts: temp, description, humidity, wind speed, icon.
     */
    private WeatherInfo parseWeatherJson(String json) {
        try {
            double temp = extractDouble(json, "\"temp\":");
            double humidity = extractDouble(json, "\"humidity\":");
            double windSpeed = extractDouble(json, "\"speed\":");
            String description = extractString(json, "\"description\":\"", "\"");
            String icon = extractString(json, "\"icon\":\"", "\"");
            String cityName = extractString(json, "\"name\":\"", "\"");

            return new WeatherInfo(temp, description, humidity, windSpeed, icon, cityName);
        } catch (Exception e) {
            System.err.println("❌ Failed to parse weather JSON: " + e.getMessage());
            return null;
        }
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-'))
            end++;
        return Double.parseDouble(json.substring(start, end));
    }

    private String extractString(String json, String startKey, String endChar) {
        int idx = json.indexOf(startKey);
        if (idx == -1) return "";
        int start = idx + startKey.length();
        int end = json.indexOf(endChar, start);
        return end > start ? json.substring(start, end) : "";
    }

    /**
     * Determines if today is a good day for maintenance based on weather.
     * Returns a recommendation message.
     *
     * @param city location of the equipment
     * @return maintenance recommendation string
     */
    public String getMaintenanceRecommendation(String city) {
        WeatherInfo weather = getCurrentWeather(city);
        if (weather == null) return "⚠️ Could not retrieve weather data.";

        String desc = weather.getDescription().toLowerCase();
        double temp = weather.getTemperature();
        double wind = weather.getWindSpeed();

        if (desc.contains("rain") || desc.contains("storm") || desc.contains("drizzle")) {
            return "🌧️ Not recommended — Rain/Storm expected. Postpone outdoor maintenance.";
        } else if (wind > 40) {
            return "💨 Not recommended — Strong winds (" + wind + " km/h). Risk of accidents.";
        } else if (temp > 38) {
            return "🌡️ Caution — Very high temperature (" + temp + "°C). Plan for early morning.";
        } else if (temp < 0) {
            return "❄️ Caution — Freezing temperature (" + temp + "°C). Check equipment for frost.";
        } else {
            return "✅ Good day for maintenance! " + weather.getDescription() + ", " + temp + "°C";
        }
    }

    /**
     * Returns icon URL for display in JavaFX ImageView.
     */
    public String getIconUrl(String iconCode) {
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }

    // ============================================================
    //  Inner class: WeatherInfo
    // ============================================================
    public static class WeatherInfo {
        private final double temperature;
        private final String description;
        private final double humidity;
        private final double windSpeed;
        private final String iconCode;
        private final String cityName;

        public WeatherInfo(double temperature, String description, double humidity,
                           double windSpeed, String iconCode, String cityName) {
            this.temperature = temperature;
            this.description = description;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.iconCode = iconCode;
            this.cityName = cityName;
        }

        public double getTemperature() { return temperature; }
        public String getDescription() { return description; }
        public double getHumidity() { return humidity; }
        public double getWindSpeed() { return windSpeed; }
        public String getIconCode() { return iconCode; }
        public String getCityName() { return cityName; }

        @Override
        public String toString() {
            return cityName + ": " + description + ", " + temperature + "°C, Humidity: " + humidity + "%";
        }
    }
}
