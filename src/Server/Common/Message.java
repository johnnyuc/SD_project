package Server.Common;

import java.net.URL;
import java.util.List;

public class Message {
    private URL mainURL;
    private String title;
    private String description;
    private List<URL> urlsInPage;
    private List<String> tokens;

    // Constructor
    public Message(URL mainURL, String title, String description, List<URL> urlsInPage, List<String> tokens) {
        this.mainURL = mainURL;
        this.title = title;
        this.description = description;
        this.urlsInPage = urlsInPage;
        this.tokens = tokens;
    }

    // Getters and Setters
    public URL getMainURL() {
        return mainURL;
    }

    public void setMainURL(URL mainURL) {
        this.mainURL = mainURL;
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

    public List<URL> getUrlsInPage() {
        return urlsInPage;
    }

    public void setUrlsInPage(List<URL> urlsInPage) {
        this.urlsInPage = urlsInPage;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }
}