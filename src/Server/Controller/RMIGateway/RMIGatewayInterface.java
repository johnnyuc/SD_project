package Server.Controller.RMIGateway;

import Client.RMIClientInterface;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RMIGatewayInterface extends Remote {
    List<String> searchQuery(String query, int pageNumber) throws RemoteException, MalformedURLException;

    void receivePing(int barrelID, String barrelIP)
            throws RemoteException, NotBoundException, MalformedURLException;

    String mostSearched() throws RemoteException;

    String barrelsStatus() throws RemoteException;

    void addObserver(RMIClientInterface observer) throws RemoteException;

    void removeObserver(RMIClientInterface observer) throws RemoteException;

    void removeBarrel(int barrelID) throws RemoteException;
}
