package Server.URLQueue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Represents a URL queue that stores URLs and provides methods to enqueue and
 * dequeue URLs.
 */
public class URLQueue extends UnicastRemoteObject implements URLQueueInterface {
    public static final String REMOTE_REFERENCE_NAME = "urlqueue";
    public static final int PORT = 5998;
    private int debug = 0;
    private final BlockingDeque<URL> urlQueue;
    private final BloomFilter bloomFilter;

    /**
     * The main method that starts the URL Queue server.
     * 
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        try {
            System.out.println("Starting URL Queue...");
            URLQueue urlQueue = new URLQueue();
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind(REMOTE_REFERENCE_NAME, urlQueue);
            System.out.println("URL Queue ready.");
        } catch (RemoteException e) {
            System.out.println("Error in Server.URLQueue.main: " + e);
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
        urlQueue = new LinkedBlockingDeque<>();

        // Number of elements and false positive probability
        int n = 5000;
        double p = 0.01;

        // Calculate the optimal size based on the number of elements and false positive
        // probability
        int optimalSize = (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
        System.out.println("Optimal size for Bloom Filter set at: " + optimalSize);

        bloomFilter = new BloomFilter(optimalSize);

        try {
            enqueueURL(URI.create("https://books.toscrape.com/").toURL(), -1);
        } catch (MalformedURLException e) {
            System.out.println("Error in Server.URLQueue.Server.URLQueue: " + e);
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
            System.out.println("Queueing URL " + url + " from downloader " + downloaderID + ".");
            bloomFilter.add(urlString);
            urlQueue.addLast(url);
            debug++;
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
        System.out.println("Priority Queueing URL " + url + ".");
        urlQueue.addFirst(url);
        debug++;
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
            System.out.println("Dequeueing URL " + url + " to downloader " + downloaderID + ".");
        } catch (InterruptedException e) {
            System.out.println("Error in Server.URLQueue.dequeueURL: " + e);
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
        System.out.println("Queue has " + count + " URLs.");
        System.out.println("Counter: " + debug);
    }
}