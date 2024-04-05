package Server.URLQueue;

import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The URLQueueInterface defines the methods for interacting with a URL queue in
 * a distributed system.
 * This interface extends the Remote interface to support remote method
 * invocation (RMI).
 */
public interface URLQueueInterface extends Remote {
    /**
     * Enqueues a URL into the queue with the specified downloader ID.
     * 
     * @param url          The URL to enqueue.
     * @param downloaderID The ID of the downloader.
     * @throws RemoteException if a remote communication error occurs.
     */
    void enqueueURL(URL url, int downloaderID) throws RemoteException;

    /**
     * Dequeues a URL from the queue for the specified downloader ID.
     * 
     * @param downloaderID The ID of the downloader.
     * @return The dequeued URL.
     * @throws RemoteException if a remote communication error occurs.
     */
    URL dequeueURL(int downloaderID) throws RemoteException;

    /**
     * Enqueues a URL into the queue with priority.
     * 
     * @param url The URL to enqueue with priority.
     * @throws RemoteException if a remote communication error occurs.
     */
    public void priorityEnqueueURL(URL url) throws RemoteException;
}
