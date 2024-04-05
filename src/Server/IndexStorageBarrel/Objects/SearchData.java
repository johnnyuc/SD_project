package Server.IndexStorageBarrel.Objects;

/**
 * Represents the search data for a specific document.
 */
public record SearchData(String url, String title, String description, double tfIdf, int refCount) {
}