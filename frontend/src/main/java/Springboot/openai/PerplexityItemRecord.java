package Springboot.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents the response structure for a Perplexity API request.
 */
public record PerplexityItemRecord(List<Choice> choices) {

    /**
     * Represents a choice made by the Perplexity API.
     */
    public static record Choice(@JsonProperty("finish_reason") String finishReason, Message message) {
    }

    /**
     * Represents a message within a choice.
     */
    public static record Message(String role, String content) {
    }
}
