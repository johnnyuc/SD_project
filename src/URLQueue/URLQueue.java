package URLQueue;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 
 */
public class URLQueue extends UnicastRemoteObject implements URLQueueInterface {
    public static void main(String args[]) {
        try {
            System.out.println("Starting URL Queue...");
            URLQueue urlQueue = new URLQueue();
            Registry registry = LocateRegistry.createRegistry(6000);
            registry.rebind("urlqueue", urlQueue);
            System.out.println("URL Queue ready.");
        } catch (RemoteException re) {
            System.out.println("Exception in URLQueue.main: " + re);
        }
    }

    private ConcurrentLinkedDeque<URL> urlQueue;

    private URLQueue() throws RemoteException {
        super();
        urlQueue = new ConcurrentLinkedDeque<>();

        try {
            enqueueURL(new URL("https://books.toscrape.com"), -1);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Enqueues a URL. This function will be used by downloaders in remote accesses
     * 
     * @param url          URL to be added
     * @param downloaderID ID of the downloader that sent the url
     * @return True on success, false otherwise
     */
    public void enqueueURL(URL url, int downloaderID) {
        System.out.println("Queueing URL " + url + " from downloader " + downloaderID + ".");
        urlQueue.addLast(url);
    }

    public URL dequeueURL(int downloaderID) {
        URL url = urlQueue.removeFirst();
        System.out.println("Dequeueing URL " + url + " to downloader " + downloaderID + ".");
        return url;
    }
}
