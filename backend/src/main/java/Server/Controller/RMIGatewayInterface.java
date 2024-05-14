package Server.Controller;

// General imports
import java.util.List;

import Server.Controller.Objects.Stats;
import Server.IndexStorageBarrel.Objects.SearchData;

import java.rmi.Remote;

// Exception imports
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

/**
 * The RMIGatewayInterface interface defines the methods that can be invoked
 * remotely by clients.
 * It extends the Remote interface, which is the marker interface for remote
 * objects.
 */
public interface RMIGatewayInterface extends Remote {
    /**
     * Searches for a query and returns a list of search results.
     * 
     * @param query      the search query
     * @param pageNumber the page number of the search results
     * @return a list of search results
     * @throws RemoteException       if a remote communication error occurs
     * @throws MalformedURLException if the URL is malformed
     */
    List<SearchData> searchQuery(String query, int pageNumber) throws RemoteException, MalformedURLException;

    /**
     * Retrieves a list of websites that are linking to the target URL.
     * 
     * @param targetUrl  the target URL
     * @param pageNumber the page number of the search results
     * @return a list of websites linking to the target URL
     * @throws RemoteException if a remote communication error occurs
     */
    List<SearchData> getWebsitesLinkingTo(String targetUrl, int pageNumber) throws RemoteException;

    /**
     * Receives a ping from a barrel with the specified ID and IP address.
     * 
     * @param barrelID the ID of the barrel
     * @param barrelIP the IP address of the barrel
     * @throws RemoteException       if a remote communication error occurs
     * @throws NotBoundException     if the barrel is not bound
     * @throws MalformedURLException if the URL is malformed
     */
    void receivePing(int barrelID, String barrelIP)
            throws RemoteException, NotBoundException, MalformedURLException;

    /**
     * Removes a barrel with the specified ID.
     * 
     * @param barrelID the ID of the barrel to be removed
     * @throws RemoteException if a remote communication error occurs
     */
    void removeBarrel(int barrelID) throws RemoteException;

    /**
     * Retrieves a list of the most searched queries.
     * 
     * @return a list of the most searched queries
     * @throws RemoteException if a remote communication error occurs
     */
    List<String> mostSearched() throws RemoteException;

    /**
     * Retrieves the status of the barrels.
     * 
     * @return the status of the barrels
     * @throws RemoteException if a remote communication error occurs
     */
    List<String> barrelsStatus() throws RemoteException;

    /**
     * Retrieves the number of active barrels.
     *
     * @return the number of active barrels
     * @throws RemoteException if a remote communication error occurs
     */
    int getActiveBarrels() throws RemoteException;

    /**
     * Enqueues a URL with priority.
     *
     * @param url The URL to enqueue.
     * @throws RemoteException if a remote communication error occurs
     */
    public void priorityEnqueueURL(String url) throws RemoteException;

    /**
     * Retrieves the stats of the server.
     *
     * @return the stats of the server
     * @throws RemoteException if a remote communication error occurs
     */
    public Stats getStats() throws RemoteException;
}
