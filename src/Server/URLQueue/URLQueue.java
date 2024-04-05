package Server.URLQueue;

/* GOOGLE GUAVA IMPLEMENTATION
import com.google.common.hash.Funnels;
import com.google.common.hash.BloomFilter;
import java.nio.charset.StandardCharsets;
*/

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class URLQueue extends UnicastRemoteObject implements URLQueueInterface {
    public static final String REMOTE_REFERENCE_NAME = "urlqueue";
    public static final int PORT = 5998;
    private int debug = 0;
    private final BlockingDeque<URL> urlQueue;
    // private final BloomFilter<CharSequence> bloomFilter;
    private final BloomFilter bloomFilter;

    // Main method
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

    // Constructor
    private URLQueue() throws RemoteException {
        super();
        urlQueue = new LinkedBlockingDeque<>();

        /* GOOGLE GUAVA IMPLEMENTATION */
        // Bloom filter with 1M elements and 0.01 false positive probability
        // https://www.baeldung.com/guava-bloom-filter
        // bloomFilter =
        // BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1000000,
        // 0.01);

        // Number of elements and false positive probability
        int n = 5000;
        double p = 0.01;

        // Calculate the optimal size based on the number of elements and false positive
        // probability
        // https://hur.st/bloomfilter
        int optimalSize = (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
        System.out.println("Optimal size for Bloom Filter set at: " + optimalSize);

        bloomFilter = new BloomFilter(optimalSize);

        try {
            enqueueURL(URI.create("https://books.toscrape.com/").toURL(), -1);
        } catch (MalformedURLException e) {
            System.out.println("Error in Server.URLQueue.Server.URLQueue: " + e);
        }
    }

    // Method to add a URL to the queue
    public void enqueueURL(URL url, int downloaderID) throws RemoteException {
        String urlString = url.toString();

        // Check if the URL is already in the queue by checking the Bloom filter
        if (!bloomFilter.contains(urlString)) {
            System.out.println("Queueing URL " + url + " from downloader " + downloaderID + ".");
            bloomFilter.add(urlString);
            urlQueue.addLast(url);
            debug++;
        }

        /*
         * GOOGLE GUAVA IMPLEMENTATION
         * if (!bloomFilter.mightContain(urlString)) {
         * System.out.println("Queueing URL " + url + " from downloader " + downloaderID
         * + ".");
         * bloomFilter.put(urlString);
         * urlQueue.add(url);
         * debug++;
         * }
         */
    }

    public void priorityEnqueueURL(URL url) throws RemoteException {
        // Don't use the bloom filter for priority URLs
        System.out.println("Priority Queueing URL " + url + ".");
        urlQueue.addFirst(url);
        debug++;
    }

    // Method to remove a URL from the queue
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

    // Show how many URLs are in the queue which are not null
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