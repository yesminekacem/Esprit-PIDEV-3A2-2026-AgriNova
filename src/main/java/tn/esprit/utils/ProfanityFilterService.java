package tn.esprit.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProfanityFilterService {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static String clean(String text) {
        try {
            String url = "https://www.purgomalum.com/service/plain?text="
                    + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return text; // fallback if API fails
        }
    }
}