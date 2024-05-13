package Server.IndexStorageBarrel.Objects;

import java.io.Serializable;

/**
 * Represents the search data for a specific document.
 * 
 * @param url         for the document
 * @param title       for the document
 * @param description for the document
 * @param tfIdf       for the term frequency-inverse document frequency value
 * @param refCount    for the count of references to the document
 */

public record SearchData(String url, String title, String description, double tfIdf, int refCount)
        implements Serializable {
    public static int EXCEPTION = -1;
}