package tn.esprit.utils;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroqAiService {

    // Groq OpenAI-compatible endpoint
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    // Good general model (you can change later)
    private static final String MODEL = "llama-3.3-70b-versatile";

    public record Suggestion(String title, List<String> tags) {}

    public static Suggestion generateTitleAndTags(String content) throws Exception {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GROQ_API_KEY environment variable.");
        }

        String system = """
                You generate forum titles and tags for a farming/agriculture forum.
                Output MUST be exactly two lines:
                TITLE: <one short title, max 70 chars>
                TAGS: <3-6 tags, comma-separated>
                """;

        String user = "Post content:\n" + content;

        String json = """
                {
                  "model": "%s",
                  "temperature": 0.4,
                  "max_tokens": 120,
                  "messages": [
                    {"role":"system","content":%s},
                    {"role":"user","content":%s}
                  ]
                }
                """.formatted(
                MODEL,
                toJsonString(system),
                toJsonString(user)
        );

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
            throw new RuntimeException("Groq API error " + resp.statusCode() + ": " + resp.body());
        }

        // Extract assistant content from OpenAI-style JSON:
        // choices[0].message.content
        String assistantText = extractJsonString(resp.body(), "\"content\"\\s*:\\s*\"");

        if (assistantText == null || assistantText.isBlank()) {
            // If parsing fails, return fallback
            return new Suggestion("Suggested title", List.of("farming", "tips", "discussion"));
        }

        // JSON unescape minimal (we only need newlines/quotes/backslashes)
        assistantText = jsonUnescape(assistantText);

        return parseSuggestion(assistantText);
    }

    private static Suggestion parseSuggestion(String raw) {
        String title = extractLine(raw, "TITLE:");
        String tagsLine = extractLine(raw, "TAGS:");

        if (title == null) title = "Suggested title";

        List<String> tags = new ArrayList<>();
        if (tagsLine != null) {
            for (String t : tagsLine.split(",")) {
                String s = t.trim();
                if (!s.isEmpty()) tags.add(s);
            }
        }
        if (tags.isEmpty()) tags = List.of("farming", "tips", "discussion");

        // keep at most 6
        if (tags.size() > 6) tags = tags.subList(0, 6);

        return new Suggestion(title.trim(), tags);
    }

    private static String extractLine(String raw, String prefix) {
        Pattern p = Pattern.compile("(?im)^\\s*" + Pattern.quote(prefix) + "\\s*(.+)$");
        Matcher m = p.matcher(raw);
        return m.find() ? m.group(1).trim() : null;
    }

    // Very small helper: finds the first JSON string value after a given regex prefix.
    // This works because OpenAI-compatible responses contain "content":"...."
    private static String extractJsonString(String json, String keyPrefixRegex) {
        Pattern p = Pattern.compile(keyPrefixRegex + "((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n") + "\"";
    }

    private static String jsonUnescape(String s) {
        return s.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
    public static String correctGrammar(String content) throws Exception {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GROQ_API_KEY environment variable.");
        }

        String system = """
            You correct grammar and spelling.
            Keep the SAME meaning.
            Keep the SAME language used by the user (English or French).
            Do NOT add extra explanations.
            Return ONLY the corrected text, no quotes, no labels.
            """;

        String user = content;

        String json = """
            {
              "model": "%s",
              "temperature": 0.2,
              "max_tokens": 500,
              "messages": [
                {"role":"system","content":%s},
                {"role":"user","content":%s}
              ]
            }
            """.formatted(
                MODEL,
                toJsonString(system),
                toJsonString(user)
        );

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
            throw new RuntimeException("Groq API error " + resp.statusCode() + ": " + resp.body());
        }

        String assistantText = extractJsonString(resp.body(), "\"content\"\\s*:\\s*\"");
        if (assistantText == null) return "";

        return jsonUnescape(assistantText).trim();
    }
}