package Test_ReliableMulticast;

// Imports
import java.util.List;
import java.net.URL;
import java.io.Serializable;

// MessageType class
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
}
