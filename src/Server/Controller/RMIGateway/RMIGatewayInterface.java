package Server.Controller.RMIGateway;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RMIGatewayInterface extends Remote {
    public List<String> searchQuery(String query, int pageNumber) throws RemoteException, MalformedURLException;

    public List<String> getWebsitesLinkingTo(String targetUrl, int pageNumber) throws RemoteException;

    public void receivePing(int barrelID, String barrelIP)
            throws RemoteException, NotBoundException, MalformedURLException;

    public void removeBarrel(int barrelID) throws RemoteException;

    public List<String> mostSearched() throws RemoteException;

    public String barrelsStatus() throws RemoteException;
}
