package Springboot.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents the response structure for a Perplexity API request.
 * @param choices The choices made by the Perplexity API.
 */
public record PerplexityItemRecord(List<Choice> choices) {

    /**
     * Represents a choice made by the Perplexity API.
     * @param finishReason The reason the choice was made.
     * @param message The message within the choice.
     */
    public static record Choice(@JsonProperty("finish_reason") String finishReason, Message message) {
    }

    /**
     * Represents a message within a choice.
     * @param role The role of the message.
     * @param content The content of the message.
     */
    public static record Message(String role, String content) {
    }
}
