package Springboot.hackernews;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import Springboot.hackernews.HackerNewsSearchRecord.Hit;

public class HackerNews {

    public static List<String> getTopStories(String query) {
        List<String> topStoriesURLs = new ArrayList<>();

        String topStoriesEndpoint = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";
        String searchStoriesEndpoint = "https://hn.algolia.com/api/v1/search?hitsPerPage=100&query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);

        RestTemplate restTemplate = new RestTemplate();
        List<Integer> hackerNewsTopStories = Arrays
                .asList(restTemplate.getForObject(topStoriesEndpoint, Integer[].class));

        HackerNewsSearchRecord hackerNewsSearchResults = restTemplate.getForObject(searchStoriesEndpoint,
                HackerNewsSearchRecord.class);

        for (Hit searchResult : hackerNewsSearchResults.hits())
            if (hackerNewsTopStories.contains(searchResult.storyId()))
                topStoriesURLs.add(searchResult.url());

        return topStoriesURLs;
    }
}
