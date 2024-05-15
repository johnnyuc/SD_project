package Springboot.hackernews;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an item record retrieved from Hacker News API.
 * @param id The item's unique id.
 * @param deleted true if the item is deleted.
 * @param type The type of item. One of "job", "story", "comment", "poll", or "pollopt".
 * @param by The username of the item's author.
 * @param time Creation date of the item, in Unix Time.
 * @param text The comment, story, or poll text. HTML.
 * @param dead true if the item is dead.
 * @param parent The comment's parent: either another comment or the relevant story.
 * @param poll The pollopt's associated poll.
 * @param kids The ids of the item's comments, in ranked display order.
 * @param url The URL of the story.
 * @param score The story's score, or the votes for a pollopt.
 * @param title The title of the story, poll, or job. HTML.
 * @param parts A list of related pollopts, in display order.
 * @param descendants In the case of stories or polls, the total comment count.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HackerNewsItemRecord(
        Integer id, // The item's unique id.
        Boolean deleted, // true if the item is deleted.
        String type, // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
        String by, // The username of the item's author.
        Long time, // Creation date of the item, in Unix Time.
        String text, // The comment, story, or poll text. HTML.
        Boolean dead, // true if the item is dead.
        String parent, // The comment's parent: either another comment or the relevant story.
        Integer poll, // The pollopt's associated poll.
        List<Integer> kids, // The ids of the item's comments, in ranked display order.
        String url, // The URL of the story.
        Integer score, // The story's score, or the votes for a pollopt.
        String title, // The title of the story, poll, or job. HTML.
        List<Integer> parts, // A list of related pollopts, in display order.
        Integer descendants // In the case of stories or polls, the total comment count.
) {
}
