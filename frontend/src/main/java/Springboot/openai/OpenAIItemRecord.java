package Springboot.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenAIItemRecord(String id, String object, long created, String model,
        @JsonProperty("system_fingerprint") String systemFingerprint,
        List<Choice> choices,
        Usage usage) {

    public static record Choice(int index, Message message, Object logprobs, String finishReason) {
    }

    public static record Message(String role, String content) {
    }

    public static record Usage(@JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens) {
    }
}