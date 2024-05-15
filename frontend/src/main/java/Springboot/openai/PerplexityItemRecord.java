package Springboot.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents the response structure for a Perplexity API request.
 */
public record PerplexityItemRecord(
        /**
         * The choices made by the Perplexity API.
         */
        List<Choice> choices) {

    /**
     * Represents a choice made by the Perplexity API.
     */
    public static record Choice(
            /**
             * The reason for finishing the choice.
             */
            @JsonProperty("finish_reason") String finishReason,

            /**
             * The message associated with the choice.
             */
            Message message) {
    }

    /**
     * Represents a message within a choice.
     */
    public static record Message(
            /**
             * The role of the message.
             */
            String role,

            /**
             * The content of the message.
             */
            String content) {
    }
}
