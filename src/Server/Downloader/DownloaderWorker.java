package Server.Downloader;// Java imports

import java.io.IOException;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import Server.URLQueue.URLQueue;
// URL imports
import Server.URLQueue.URLQueueInterface;
import ReliableMulticast.ReliableMulticast;
import ReliableMulticast.Objects.CrawlData;

// Jsoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import Logger.LogUtil;

public class DownloaderWorker implements Runnable {
    // Worker's ID, Server.URLQueue IP, and Server.URLQueue object
    private final int id;
    private final String queueIP;
    private URLQueueInterface urlQueue;

    // Minimum and maximum wait time for the downloader
    private final int minWaitTime;
    private final int maxWaitTime;

    private final ReliableMulticast reliableMulticast;

    // Default constructor
    public DownloaderWorker(int id, String queueIP, String multicastGroupAddress,
            int multicastPort, int minWaitTime, int maxWaitTime) {
        this.id = id;
        this.queueIP = queueIP;
        this.minWaitTime = minWaitTime;
        this.maxWaitTime = maxWaitTime;
        this.reliableMulticast = new ReliableMulticast(multicastGroupAddress, multicastPort);

        // Add ctrl+c shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            // Connect to the Server.URLQueue
            urlQueue = (URLQueueInterface) LocateRegistry.getRegistry(URLQueue.PORT)
                    .lookup(URLQueue.REMOTE_REFERENCE_NAME);

            reliableMulticast.startReceiving();

            while (true)
                visitURL(urlQueue.dequeueURL(id));

        } catch (NotBoundException | IOException e) {
            LogUtil.logError(LogUtil.ANSI_RED, DownloaderWorker.class, e);
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
                        System.out.println(href);
                        URL urlObject = URI.create(href).toURL();
                        URI uri = new URI(urlObject.getProtocol(), urlObject.getUserInfo(), urlObject.getHost(),
                                urlObject.getPort(), urlObject.getPath(), urlObject.getQuery(), urlObject.getRef());
                        urlQueue.enqueueURL(uri.toURL(), id);
                        urlList.add(uri.toURL());
                    } catch (URISyntaxException | MalformedURLException e) {
                        LogUtil.logError(LogUtil.ANSI_RED, DownloaderWorker.class, e);
                    }
                }
            }

            // Create a CrawlData object
            CrawlData crawlData = new CrawlData(url, doc.title(), doc.text(), tokenList, urlList);

            // Send the crawling data via reliable multicast
            LogUtil.logInfo(LogUtil.ANSI_WHITE, DownloaderWorker.class,
                    "Sending data to barrel: " + crawlData.getUrl());
            reliableMulticast.send(crawlData);

            // Calculate the wait time based on the response time
            // Auto throttle the downloader
            int waitTime = (int) Math.min(maxWaitTime, Math.max(minWaitTime, responseTime * 1.5));
            Thread.sleep(waitTime);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error in Server.Downloader.Server.Downloader.visitURL: " + e);
            System.out.println("URL: " + url);
        }
    }

    private void stop() {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, DownloaderWorker.class, "Shutting down Downloader Worker " + id);
        reliableMulticast.stopSending();
        reliableMulticast.stopReceiving();
    }
}