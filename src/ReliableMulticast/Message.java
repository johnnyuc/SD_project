package ReliableMulticast;

// Imports
import java.util.List;
import java.net.URL;
import java.io.Serializable;

// Message class
public class Message implements Serializable {
    // Fields
    public URL url;
    public String title;
    public String description;
    public List<String> tokens;
    public List<URL> urlStrings;

    // Constructor
    public Message(URL url, String title, String description, List<String> tokens, List<URL> urlStrings) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.tokens = tokens;
        this.urlStrings = urlStrings;
    }

    // Getters and Setters
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public List<URL> getUrlStrings() {
        return urlStrings;
    }

    public void setUrlStrings(List<URL> urlStrings) {
        this.urlStrings = urlStrings;
    }
}
