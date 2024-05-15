package Springboot.hackernews;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an item retrieved from the Hacker News API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HackerNewsItemRecord(
        /**
         * The item's unique id.
         */
        Integer id,

        /**
         * True if the item is deleted.
         */
        Boolean deleted,

        /**
         * The type of item: "job", "story", "comment", "poll", or "pollopt".
         */
        String type,

        /**
         * The username of the item's author.
         */
        String by,

        /**
         * Creation date of the item, in Unix Time.
         */
        Long time,

        /**
         * The comment, story, or poll text in HTML format.
         */
        String text,

        /**
         * True if the item is dead.
         */
        Boolean dead,

        /**
         * The comment's parent: either another comment or the relevant story.
         */
        String parent,

        /**
         * The pollopt's associated poll.
         */
        Integer poll,

        /**
         * The ids of the item's comments, in ranked display order.
         */
        List<Integer> kids,

        /**
         * The URL of the story.
         */
        String url,

        /**
         * The story's score or the votes for a pollopt.
         */
        Integer score,

        /**
         * The title of the story, poll, or job in HTML format.
         */
        String title,

        /**
         * A list of related pollopts in display order.
         */
        List<Integer> parts,

        /**
         * Total comment count for stories or polls.
         */
        Integer descendants) {
}
