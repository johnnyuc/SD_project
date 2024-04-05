package Server.IndexStorageBarrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IndexStorageBarrelInterface extends Remote {
    public List<String> searchQuery(String query, int pageNumber) throws RemoteException;

    public List<String> getWebsitesLinkingTo(String targetUrl, int pageNumber) throws RemoteException;

    public List<String> getTopSearches() throws RemoteException;

    public void receivePing() throws RemoteException;
}
