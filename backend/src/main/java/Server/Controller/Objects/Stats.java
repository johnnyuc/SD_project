package Server.Controller.Objects;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents statistics data including barrel stats and most searched terms.
 */
public record Stats(
        @JsonProperty("barrel_stats") List<String> barrelStats,
        @JsonProperty("most_searched") List<String> mostSearched) implements Serializable {
}
