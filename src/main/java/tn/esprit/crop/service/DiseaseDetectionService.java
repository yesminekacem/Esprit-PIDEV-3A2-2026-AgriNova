package tn.esprit.crop.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DiseaseDetectionService {

    private static final String API_URL = "http://127.0.0.1:5000/detect";
    private final HttpClient client = HttpClient.newHttpClient();

    public String sendImageToAI(File imageFile)
            throws IOException, InterruptedException {

        String boundary = "Boundary-" + System.currentTimeMillis();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(buildMultipartBody(imageFile, boundary))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private HttpRequest.BodyPublisher buildMultipartBody(File file, String boundary)
            throws IOException {

        List<byte[]> byteArrays = new ArrayList<>();

        String fileName = file.getName();
        String mimeType = Files.probeContentType(file.toPath());

        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        byteArrays.add(("--" + boundary + "\r\n").getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"\r\n").getBytes());
        byteArrays.add(("Content-Type: " + mimeType + "\r\n\r\n").getBytes());
        byteArrays.add(Files.readAllBytes(file.toPath()));
        byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}