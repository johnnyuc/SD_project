package Springboot.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PerplexityItemRecord(List<Choice> choices) {

    public static record Choice(@JsonProperty("finish_reason") String finishReason, Message message) {
    }

    public static record Message(String role, String content) {
    }
}