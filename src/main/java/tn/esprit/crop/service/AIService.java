package tn.esprit.crop.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIService {

    public String generateTasks(String json) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:5000/generate-tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}