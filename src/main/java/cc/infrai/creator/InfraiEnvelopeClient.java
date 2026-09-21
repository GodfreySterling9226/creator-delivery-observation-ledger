package cc.infrai.creator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

final class InfraiEnvelopeClient {
    private final InfraiProperties properties;
    private final HttpClient http;

    InfraiEnvelopeClient(InfraiProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newHttpClient();
    }

    int countTokens(String content) throws IOException, InterruptedException {
        String body = "{\"model\":\"auto\",\"messages\":[{\"role\":\"user\",\"content\":\""
                + escape(content) + "\"}]}";
        String data = post("/v1/ai/tokens/count", body);
        return integer(data, "count").orElseGet(() -> integer(data, "tokens").orElse(0));
    }

    void captureException(Exception failure) throws IOException, InterruptedException {
        String body = "{\"exception\":{\"type\":\"" + escape(failure.getClass().getSimpleName())
                + "\",\"message\":\"" + escape(Optional.ofNullable(failure.getMessage()).orElse("delivery failed")) + "\"}}";
        post("/v1/errors/capture", body);
    }

    private String post(String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + path))
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.decode(response.body());
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryDelay(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiRejectedException(envelope.error(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("remote request was not accepted");
            }
            return envelope.data();
        }
        throw new IOException("request retries exhausted");
    }

    private static long retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1000L)
                .orElse(250L * (1L << attempt));
    }

    private static Optional<Integer> integer(String json, String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + name + "\\\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private record Envelope(boolean ok, String data, String error) {
        static Envelope decode(String json) {
            boolean ok = json.matches("(?s).*\\\"ok\\\"\\s*:\\s*true.*");
            String error = value(json, "error").orElse("request rejected");
            String data = value(json, "data").orElse(json);
            return new Envelope(ok, data, error);
        }

        private static Optional<String> value(String json, String field) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?s)\\\"" + field + "\\\"\\s*:\\s*(\\{.*?\\}|\\[.*?\\]|\\\".*?\\\"|\\d+)")
                    .matcher(json);
            return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
        }
    }

    static final class InfraiRejectedException extends RuntimeException {
        private final int status;

        InfraiRejectedException(String error, int status) {
            super(error);
            this.status = status;
        }

        int status() {
            return status;
        }
    }
}
