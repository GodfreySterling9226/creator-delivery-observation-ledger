package cc.infrai.creator;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

final class CreatorDeliveryService {
    private final OpenAIClient ai;
    private final InfraiEnvelopeClient observations;

    CreatorDeliveryService(InfraiProperties properties) {
        this.ai = OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .baseUrl(properties.baseUrl())
                .build();
        this.observations = new InfraiEnvelopeClient(properties);
    }

    DeliveryReceipt deliver(String assetId, String subscriber, String assetSummary) {
        int tokens = 0;
        try {
            tokens = observations.countTokens(assetSummary);
            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of("auto"))
                    .addUserMessage("Write one concise subscriber update for " + subscriber + ": " + assetSummary)
                    .build();
            ChatCompletion completion = ai.chat().completions().create(request);
            String copy = completion.choices().get(0).message().content().orElse("Your asset is ready.");
            return new DeliveryReceipt(assetId, shouldNotify(copy), tokens, copy);
        } catch (Exception failure) {
            try {
                observations.captureException(failure);
            } catch (Exception captureFailure) {
                throw new IllegalStateException("delivery request rejected", captureFailure);
            }
            throw new DeliveryFailedException(tokens, failure);
        }
    }

    static boolean shouldNotify(String copy) {
        return copy != null && !copy.isBlank();
    }

    static final class DeliveryFailedException extends RuntimeException {
        private final int countedTokens;

        DeliveryFailedException(int countedTokens, Exception cause) {
            super("asset delivery rejected after token accounting", cause);
            this.countedTokens = countedTokens;
        }

        int countedTokens() {
            return countedTokens;
        }
    }
}
