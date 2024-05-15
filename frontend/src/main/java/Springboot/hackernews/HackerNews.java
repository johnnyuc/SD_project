package Springboot.hackernews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.client.RestTemplate;

import Server.Downloader.Crawling;

/**
 * Provides functionality to interact with Hacker News API and fetch top
 * stories.
 */
public class HackerNews {

    /**
     * Fetches the top stories from Hacker News and filters them based on the
     * provided query.
     *
     * @param query the query to search for in the top stories' content
     * @return a list of URLs of the top stories that contain the query
     */
    public static List<String> getTopStories(String query) {
        List<String> topStoriesURLs = new ArrayList<>();
        List<Integer> hackerNewsTopStories = new ArrayList<>();

        String topStoriesEndpoint = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";

        RestTemplate restTemplate = new RestTemplate();

        try {
            hackerNewsTopStories = Arrays.asList(restTemplate.getForObject(topStoriesEndpoint, Integer[].class));
        } catch (Exception e) {
            System.out.println("Error getting top stories: " + e);
            return topStoriesURLs;
        }

        // Generate 10 unique random numbers between 0 and 499
        // This is used to save processing time when searching for the top stories
        List<Integer> storyIndexes = new ArrayList<>();
        while (storyIndexes.size() < 10) {
            int randomNumber = (int) (Math.random() * 500);
            if (!storyIndexes.contains(randomNumber))
                storyIndexes.add(randomNumber);
        }

        for (int storyIndex : storyIndexes) {
            String storyURL = "https://hacker-news.firebaseio.com/v0/item/" + hackerNewsTopStories.get(storyIndex)
                    + ".json?print=pretty";

            try {
                HackerNewsItemRecord oneStory = restTemplate.getForObject(storyURL, HackerNewsItemRecord.class);

                if (oneStory == null || !Crawling.isValidURL(oneStory.url()))
                    continue;

                Document doc = Jsoup.connect(oneStory.url().toString()).get();
                List<String> tokens = Crawling.getTokens(doc);

                if (!query.isEmpty() && tokens.contains(query)) {
                    topStoriesURLs.add(oneStory.url());
                    System.out.println("Found query in story: " + oneStory.url());
                } else
                    System.out.println("Did not find query \"" + query + "\" in story: " + oneStory.url());
            } catch (Exception e) {
                System.out.println("Error getting story: " + e);
                continue;
            }

        }
        return topStoriesURLs;
    }
}
