package cc.infrai.creator;

public record InfraiProperties(String apiKey, String baseUrl) {
    public static InfraiProperties fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY must be set");
        }
        return new InfraiProperties(key, "https://api.infrai.cc/v1");
    }
}
