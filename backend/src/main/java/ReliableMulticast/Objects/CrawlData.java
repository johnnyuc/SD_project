package ReliableMulticast.Objects;

// General imports
import java.net.URL;
import java.util.List;
import java.io.Serializable;

/**
 * Represents the data obtained from crawling a web page.
 */
public class CrawlData implements Serializable {
    // Fields
    /**
     * The URL of the web page.
     */
    public URL url;
    /**
     * The title of the web page.
     */
    public String title;
    /**
     * The description of the web page.
     */
    public String description;
    /**
     * The list of tokens extracted from the web page.
     */
    public List<String> tokens;
    /**
     * The list of URLs found in the web page.
     */
    public List<URL> urlStrings;

    /**
     * Constructs a new CrawlData object.
     * 
     * @param url         the URL of the web page
     * @param title       the title of the web page
     * @param description the description of the web page
     * @param tokens      the list of tokens extracted from the web page
     * @param urlStrings  the list of URLs found in the web page
     */
    public CrawlData(URL url, String title, String description, List<String> tokens, List<URL> urlStrings) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.tokens = tokens;
        this.urlStrings = urlStrings;
    }

    // Getters and Setters

    /**
     * Returns the URL of the web page.
     * 
     * @return the URL of the web page
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Sets the URL of the web page.
     * 
     * @param url the URL of the web page
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * Returns the title of the web page.
     * 
     * @return the title of the web page
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the description of the web page.
     * 
     * @return the description of the web page
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the list of tokens extracted from the web page.
     * 
     * @return the list of tokens
     */
    public List<String> getTokens() {
        return tokens;
    }

    /**
     * Returns the list of URLs found in the web page.
     * 
     * @return the list of URLs
     */
    public List<URL> getUrlStrings() {
        return urlStrings;
    }

}
