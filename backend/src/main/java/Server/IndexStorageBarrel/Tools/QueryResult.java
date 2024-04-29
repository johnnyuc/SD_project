package Server.IndexStorageBarrel.Tools;

/**
 * Represents the result of a query.
 * @param websiteId The ID of the website.
 * @param newUrl Whether the URL is new.
 */
public record QueryResult(int websiteId, boolean newUrl) {
}