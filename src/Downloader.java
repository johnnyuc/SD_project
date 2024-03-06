
// Jsoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import URLQueue.URLQueueInterface;

// Java imports
import java.io.IOException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;

/**
 * Downloader
 */
public class Downloader implements Runnable {
    private final int id;
    private URLQueueInterface urlQueueInterface;

    public Downloader(int id) {
        this.id = id;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        // TODO: Treat the exception better

        try {
            urlQueueInterface = (URLQueueInterface) LocateRegistry.getRegistry(6000)
                    .lookup("urlqueue");
            for (int i = 0; i < 20; i++) {
                visitURL(urlQueueInterface.dequeueURL(id));
            }

        } catch (Exception e) {
            System.out.println("Exception in main: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Receives an URL from a queue and visits it
     * 
     * @param url URL from queue to visit
     */
    private void visitURL(URL url) {
        final Logger logger = Logger.getLogger(Downloader.class.getName());
        try {
            // Connect to the given URL
            Document doc = Jsoup.connect(url.toString()).get();
            // Tokenize the resulting document
            StringTokenizer tokens = new StringTokenizer(doc.text());
            int countTokens = 0;
            // Print the first 100 tokens found
            while (tokens.hasMoreElements() && countTokens++ < 100)
                System.out.println(tokens.nextToken().toLowerCase());
            // Find every link in the URL and print them
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                urlQueueInterface.enqueueURL(new URL(link.attr("abs:href")), id);
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error attempting to connect to URL: " + url, e);
        }
    }
}