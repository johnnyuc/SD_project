package Server.IndexStorageBarrel;

// General imports
import java.util.List;
import java.rmi.Remote;

// Exception imports
import java.rmi.RemoteException;

/**
 * The IndexStorageBarrelInterface represents the remote interface for the Index
 * Storage Barrel component.
 * It defines the methods that can be invoked remotely.
 */
public interface IndexStorageBarrelInterface extends Remote {
    /**
     * Searches the index for the given query and returns a list of matching
     * documents.
     * 
     * @param query      the search query
     * @param pageNumber the page number of the search results
     * @return a list of matching documents
     * @throws RemoteException if a remote communication error occurs
     */
    List<String> searchQuery(String query, int pageNumber) throws RemoteException;

    /**
     * Retrieves a list of websites that link to the specified target URL.
     * 
     * @param targetUrl  the target URL
     * @param pageNumber the page number of the search results
     * @return a list of websites linking to the target URL
     * @throws RemoteException if a remote communication error occurs
     */
    List<String> getWebsitesLinkingTo(String targetUrl, int pageNumber) throws RemoteException;

    /**
     * Retrieves the top searches performed by users.
     * 
     * @return a list of top searches
     * @throws RemoteException if a remote communication error occurs
     */
    List<String> getTopSearches() throws RemoteException;

    /**
     * Receives a ping from the client to check if the server is still responsive.
     * 
     * @throws RemoteException if a remote communication error occurs
     */
    void receivePing() throws RemoteException;
}
