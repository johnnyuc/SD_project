package Server.URLQueue;

// Logging imports
import Logger.LogUtil;

// General imports
import java.net.URI;
import java.net.URL;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

// Exception imports
import java.io.IOException;
import java.rmi.RemoteException;
import java.net.MalformedURLException;

/**
 * Represents a URL queue that stores URLs and provides methods to enqueue and
 * dequeue URLs.
 */
public class URLQueue extends UnicastRemoteObject implements URLQueueInterface {
    /**
     * Remote reference name for the URL queue.
     */
    public static final String REMOTE_REFERENCE_NAME = "urlqueue";
    /**
     * The RMI port number.
     */
    public static final int RMI_PORT = 5998;
    /**
     * The UDP port number.
     */
    public static final int UDP_PORT = 5997;
    /**
     * The size of the UDP buffer.
     */
    public static final int UDP_BUFFER_SIZE = 1024;

    /**
     * A flag indicating whether the URL Queue server is running.
     */
    private volatile boolean running = true;
    /**
     * The URL queue.
     */
    private final BlockingDeque<URL> urlQueue;
    /**
     * The Bloom filter used to check if a URL is already in the queue.
     */
    private final BloomFilter bloomFilter;

    /**
     * The main method that starts the URL Queue server.
     * 
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        try {
            LogUtil.logInfo(LogUtil.ANSI_YELLOW, URLQueue.class, "Starting URL Queue...");
            URLQueue urlQueue = new URLQueue();
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            registry.rebind(REMOTE_REFERENCE_NAME, urlQueue);
            LogUtil.logInfo(LogUtil.ANSI_GREEN, URLQueue.class, "URL Queue ready.");
        } catch (RemoteException e) {
            LogUtil.logError(LogUtil.ANSI_RED, URLQueue.class, e);
            System.exit(1);
        }
    }

    /**
     * Constructs a new URLQueue object.
     * 
     * @throws RemoteException If a remote exception occurs.
     */
    private URLQueue() throws RemoteException {
        super();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));

        // Create new thread to run listenIndexURLRequest
        new Thread(this::listenIndexURLRequest, "UDPListener").start();

        urlQueue = new LinkedBlockingDeque<>();

        // Number of elements and false positive probability
        int n = 5000;
        double p = 0.01;

        // Calculate the optimal size based on the number of elements and false positive
        // probability
        int optimalSize = (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
        LogUtil.logInfo(LogUtil.ANSI_BLUE, URLQueue.class, "Optimal size for Bloom Filter set at: " + optimalSize);

        bloomFilter = new BloomFilter(optimalSize);

        try {
            enqueueURL(URI.create("https://books.toscrape.com/").toURL(), -1);
        } catch (MalformedURLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, URLQueue.class, e);
        }
    }

    /**
     * Adds a URL to the queue.
     * 
     * @param url          The URL to enqueue.
     * @param downloaderID The ID of the downloader.
     * @throws RemoteException If a remote exception occurs.
     */
    public void enqueueURL(URL url, int downloaderID) throws RemoteException {
        String urlString = url.toString();

        // Check if the URL is already in the queue by checking the Bloom filter
        if (!bloomFilter.contains(urlString)) {
            LogUtil.logInfo(LogUtil.ANSI_BLUE, URLQueue.class,
                    "Queueing URL " + url + " from downloader " + downloaderID + ".");
            bloomFilter.add(urlString);
            urlQueue.addLast(url);
        }
    }

    /**
     * Listens for incoming index URL requests and enqueues them for processing.
     * This method runs in a loop until the server is stopped.
     */
    public void listenIndexURLRequest() {
        while (running) {
            try (DatagramSocket socketUDP = new DatagramSocket(UDP_PORT)) {
                byte[] buffer = new byte[UDP_BUFFER_SIZE];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(request);

                // Transform the received data into a URL
                String url = new String(request.getData(), 0, request.getLength());
                priorityEnqueueURL(URI.create(url).toURL());
            } catch (IOException e) {
                LogUtil.logError(LogUtil.ANSI_RED, URLQueue.class, e);
            }
        }
    }

    /**
     * Adds a URL to the priority queue.
     * 
     * @param url The URL to enqueue.
     * @throws RemoteException If a remote exception occurs.
     */
    public void priorityEnqueueURL(URL url) throws RemoteException {
        // Don't use the bloom filter for priority URLs
        LogUtil.logInfo(LogUtil.ANSI_BLUE, URLQueue.class, "Priority Queueing URL " + url + ".");
        urlQueue.addFirst(url);
    }

    /**
     * Removes and returns a URL from the queue.
     * 
     * @param downloaderID The ID of the downloader.
     * @return The dequeued URL.
     * @throws RemoteException If a remote exception occurs.
     */
    public URL dequeueURL(int downloaderID) throws RemoteException {
        URL url = null;
        try {
            url = urlQueue.takeFirst();
            LogUtil.logInfo(LogUtil.ANSI_BLUE, URLQueue.class,
                    "URL " + url + " dequeued by downloader " + downloaderID + ".");
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_RED, URLQueue.class, e);
        }
        showQueue();
        return url;
    }

    /**
     * Shows the number of URLs in the queue that are not null.
     */
    private void showQueue() {
        int count = 0;
        for (URL url : urlQueue) {
            if (url != null)
                count++;
        }
        LogUtil.logInfo(LogUtil.ANSI_BLUE, URLQueue.class, "Queue has " + count + " URLs.");
    }
}