// Java imports
import java.io.IOException;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.StringTokenizer;

// URL imports
import URLQueue.URLQueueInterface;

// Jsoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Downloader implements Runnable {
    private final int id;
    private final String queueIP;
    private URLQueueInterface urlQueue;

    public Downloader(int id, String queueIP) {
        this.id = id;
        this.queueIP = queueIP;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {
            urlQueue = (URLQueueInterface) LocateRegistry.getRegistry(queueIP, 6000).lookup("urlqueue");

            // Shouldn't be true, but for now it's a way to keep the downloader running
            while (true) visitURL(urlQueue.dequeueURL(id));

        } catch (NotBoundException | IOException e) {
            System.out.println("Error in Downloader.run: " + e);
        }
    }

    private void visitURL(URL url) {
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
                urlQueue.enqueueURL(URI.create(link.attr("abs:href")).toURL(), id);
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
            }
        } catch (IOException e) {
            System.out.println("Error in Downloader.visitURL: " + e);
        }
    }

}