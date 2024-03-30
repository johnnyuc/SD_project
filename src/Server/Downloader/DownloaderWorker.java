package Server.Downloader;// Java imports

import java.io.IOException;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

// URL imports
import Server.URLQueue.URLQueueInterface;
import ReliableMulticast.Objects.CrawlData;

// Jsoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DownloaderWorker implements Runnable {
    // Worker's ID, Server.URLQueue IP, and Server.URLQueue object
    private final int id;
    private final String queueIP;
    private URLQueueInterface urlQueue;

    // Minimum and maximum wait time for the downloader
    private final int minWaitTime;
    private final int maxWaitTime;

    // Default constructor
    public DownloaderWorker(int id, String queueIP, int minWaitTime, int maxWaitTime) {
        this.id = id;
        this.queueIP = queueIP;
        this.minWaitTime = minWaitTime;
        this.maxWaitTime = maxWaitTime;
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            // Connect to the Server.URLQueue
            urlQueue = (URLQueueInterface) LocateRegistry.getRegistry(queueIP, 6000).lookup("urlqueue");

            while (true)
                visitURL(urlQueue.dequeueURL(id));

        } catch (NotBoundException | IOException e) {
            System.out.println("Error in Server.Downloader.Server.Downloader.run: " + e);
        }
    }

    private void visitURL(URL url) {
        try {
            // TODO: Account for some malformed URLs (e.g., missing protocol, special
            // characters, etc.)

            // Start the timer
            long startTime = System.currentTimeMillis();

            // Connect to the given URL
            Document doc = Jsoup.connect(url.toString()).get();

            // Stop the timer and calculate the response time
            long responseTime = System.currentTimeMillis() - startTime;

            // Tokenize the resulting document
            StringTokenizer tokens = new StringTokenizer(doc.text());
            List<String> tokenList = new ArrayList<>();

            // Store all tokens in the list
            while (tokens.hasMoreElements())
                tokenList.add(tokens.nextToken().toLowerCase());

            // Find every link in the URL and print them
            Elements links = doc.select("a[href]");
            List<URL> urlList = new ArrayList<>();
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (href.startsWith("http://") || href.startsWith("https://")) {
                    try {
                        URL urlObject = URI.create(href).toURL();
                        URI uri = new URI(urlObject.getProtocol(), urlObject.getUserInfo(), urlObject.getHost(),
                                urlObject.getPort(), urlObject.getPath(), urlObject.getQuery(), urlObject.getRef());
                        urlQueue.enqueueURL(uri.toURL(), id);
                        urlList.add(uri.toURL());
                    } catch (URISyntaxException | MalformedURLException e) {
                        System.out.println("Error in Server.Downloader.Server.Downloader.visitURL: " + e);
                        System.out.println("URL: " + href);
                    }
                }
            }

            /*
             * Print all tokens in list
             * for (String token : tokenList)
             * System.out.println(token);
             */

            // Create a CrawlData object
            CrawlData crawlData = new CrawlData(url, doc.title(), doc.text(), tokenList, urlList);

            // Send the crawling data via reliable multicast
            reliableMulticast(crawlData);

            // Calculate the wait time based on the response time
            // Auto throttle the downloader
            int waitTime = (int) Math.min(maxWaitTime, Math.max(minWaitTime, responseTime * 1.5));
            Thread.sleep(waitTime);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error in Server.Downloader.Server.Downloader.visitURL: " + e);
            System.out.println("URL: " + url);
        }
    }

    private void reliableMulticast(CrawlData crawlData) {
        // TODO: To be done
    }
}