package ReliableMulticast.Objects;

// Imports
import java.net.URL;
import java.util.List;
import java.io.Serializable;

/**
 * Represents the data obtained from crawling a web page.
 */
public class CrawlData implements Serializable {
    // Fields
    public URL url;
    public String title;
    public String description;
    public List<String> tokens;
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
     * Sets the title of the web page.
     * 
     * @param title the title of the web page
     */
    public void setTitle(String title) {
        this.title = title;
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
     * Sets the description of the web page.
     * 
     * @param description the description of the web page
     */
    public void setDescription(String description) {
        this.description = description;
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
     * Sets the list of tokens extracted from the web page.
     * 
     * @param tokens the list of tokens
     */
    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    /**
     * Returns the list of URLs found in the web page.
     * 
     * @return the list of URLs
     */
    public List<URL> getUrlStrings() {
        return urlStrings;
    }

    /**
     * Sets the list of URLs found in the web page.
     * 
     * @param urlStrings the list of URLs
     */
    public void setUrlStrings(List<URL> urlStrings) {
        this.urlStrings = urlStrings;
    }
}
