package Springboot.hackernews;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.client.RestTemplate;

public class HackerNews {

    public static List<String> getTopStories(String query) {
        List<String> topStoriesURLs = new ArrayList<>();

        String topStoriesEndpoint = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";

        RestTemplate restTemplate = new RestTemplate();
        List hackerNewsNewTopStories = restTemplate.getForObject(topStoriesEndpoint, List.class);

        for (Object storyId : hackerNewsNewTopStories) {
            String storyURL = "https://hacker-news.firebaseio.com/v0/item/" + storyId + ".json?print=pretty";
            System.out.println(storyURL);
            HackerNewsItemRecord oneStory = restTemplate.getForObject(storyURL, HackerNewsItemRecord.class);

            if (query != "")
                topStoriesURLs.add(oneStory.url());
        }

        return topStoriesURLs;
    }
}
