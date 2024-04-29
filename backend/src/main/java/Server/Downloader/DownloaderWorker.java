package Server.Downloader;

// Package imports
import Server.URLQueue.URLQueue;
import Server.URLQueue.URLQueueInterface;
import ReliableMulticast.ReliableMulticast;
import ReliableMulticast.Objects.CrawlData;

// Logging imports
import Logger.LogUtil;

// General imports
import java.net.*;
import java.rmi.Naming;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.nio.charset.StandardCharsets;

// Jsoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// Exception imports
import java.io.IOException;
import java.rmi.NotBoundException;
import java.io.UnsupportedEncodingException;

/**
 * The DownloaderWorker class represents a worker that visits URLs and performs
 * crawling tasks.
 */
public class DownloaderWorker implements Runnable {
    // Worker's ID, Server.URLQueue IP, and Server.URLQueue object
    /**
     * The ID of the worker.
     */
    private final int id;
    /**
     * The IP address of the Server.URLQueue.
     */
    private final String queueIP;
    /**
     * The Server.URLQueue object.
     */
    private URLQueueInterface urlQueue;

    // Minimum and maximum wait time for the downloader
    /**
     * The minimum wait time for the downloader.
     */
    private final int minWaitTime;
    /**
     * The maximum wait time for the downloader.
     */
    private final int maxWaitTime;

    /**
     * The ReliableMulticast object for sending and receiving data.
     */
    private final ReliableMulticast reliableMulticast;

    /**
     * Constructs a DownloaderWorker object with the specified parameters.
     *
     * @param id                the ID of the worker
     * @param queueIP           the IP address of the Server.URLQueue
     * @param interfaceAddress  the interface address for reliable multicast
     * @param mcastGroupAddress the multicast group address for reliable multicast
     * @param multicastPort     the multicast port for reliable multicast
     * @param minWaitTime       the minimum wait time for the downloader
     * @param maxWaitTime       the maximum wait time for the downloader
     */
    public DownloaderWorker(int id, String queueIP, String interfaceAddress, String mcastGroupAddress,
            int multicastPort, int minWaitTime, int maxWaitTime) {
        this.id = id;
        this.queueIP = queueIP;
        this.minWaitTime = minWaitTime;
        this.maxWaitTime = maxWaitTime;
        Class<?>[] ignoredClasses = { DownloaderWorker.class };
        this.reliableMulticast = new ReliableMulticast(interfaceAddress, mcastGroupAddress, multicastPort,
                DownloaderWorker.class, ignoredClasses);

        // Add ctrl+c shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        new Thread(this, "Downloader " + id).start();
    }

    @Override
    public void run() {
        try {
            // Connect to the Server.URLQueue
            urlQueue = (URLQueueInterface) Naming
                    .lookup("rmi://" + queueIP + ":" + URLQueue.RMI_PORT + "/" + URLQueue.REMOTE_REFERENCE_NAME);

            reliableMulticast.startReceiving();

            while (true)
                visitURL(urlQueue.dequeueURL(id));

        } catch (NotBoundException | IOException e) {
            LogUtil.logInfo(LogUtil.ANSI_RED, DownloaderWorker.class, "Unable to connect to URLQueue: " + queueIP);
            stop();
        }
    }

    /**
     * Visits the specified URL and performs crawling tasks.
     *
     * @param url the URL to visit
     */
    private void visitURL(URL url) {
        try {
            String urlString = url.toString();
            urlString = getString(urlString);

            HttpURLConnection connection = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("URL not reachable or does not exist: " + url);
                return;
            }
        } catch (IOException e) {
            System.out.println("Error checking URL: " + url);
            return;
        }

        try {
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
            while (tokens.hasMoreElements()) {
                String token = tokens.nextToken().toLowerCase();
                token = token.trim();
                token = token.replaceAll("[\\[\\](){}?!,.:]", "");
                // Remove single characters
                if (token.length() > 1) {
                    // If it's a number, it should contain a comma or a dot
                    if (token.matches("\\d+")) {
                        if (token.contains(".") || token.contains(",")) {
                            tokenList.add(token);
                        }
                    } else {
                        tokenList.add(token);
                    }
                }
            }

            // Find every link in the URL and print them
            Elements links = doc.select("a[href]");
            List<URL> urlList = new ArrayList<>();
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (href.startsWith("http://") || href.startsWith("https://")) {
                    try {
                        href = getString(href);
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
            LogUtil.logInfo(LogUtil.ANSI_BLUE, DownloaderWorker.class,
                    "Sending data to barrel: " + crawlData.getUrl());
            reliableMulticast.send(crawlData);

            // Calculate the wait time based on the response time
            // Auto throttle the downloader
            int waitTime = (int) Math.min(maxWaitTime, Math.max(minWaitTime, responseTime * 1.5));
            Thread.sleep(waitTime);
        } catch (IOException e) {
            LogUtil.logInfo(LogUtil.ANSI_RED, DownloaderWorker.class, "Unable to reach URLQueue: " + queueIP);
            System.exit(1);
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_RED, DownloaderWorker.class, e);
        }
    }

    /**
     * Encodes the URL string to ensure that it is properly formatted.
     *
     * @param href the URL string to encode
     * @return the encoded URL string
     * @throws UnsupportedEncodingException if the encoding is not supported
     */
    private String getString(String href) throws UnsupportedEncodingException {
        int hashIndex = href.indexOf('#');
        if (hashIndex > -1) {
            // The URL contains a fragment, so encode it
            String fragment = href.substring(hashIndex + 1);
            String encodedFragment = URLEncoder.encode(fragment, StandardCharsets.UTF_8);
            // Replace < and > characters
            encodedFragment = encodedFragment.replace("<", "%3C").replace(">", "%3E");
            href = href.substring(0, hashIndex) + '#' + encodedFragment;
        }
        return href;
    }

    /**
     * Stops the downloader worker.
     */
    private void stop() {
        reliableMulticast.stopSending();
        reliableMulticast.stopReceiving();
    }
}