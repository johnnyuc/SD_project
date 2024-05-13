package Springboot.hackernews;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HackerNewsSearchRecord(List<Hit> hits) {
    public static record Hit(@JsonProperty("story_id") int storyId, String url) {
    }

}
