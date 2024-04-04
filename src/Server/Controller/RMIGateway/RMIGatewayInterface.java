package Server.Controller.RMIGateway;

import Client.RMIClientInterface;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGatewayInterface extends Remote {
    boolean searchQuery(String query) throws RemoteException;

    void receivePing(int barrelID, long timestamp) throws RemoteException, NotBoundException;

    String mostSearched() throws RemoteException;

    String barrelsStatus() throws RemoteException;

    void addObserver(RMIClientInterface observer) throws RemoteException;

    void removeObserver(RMIClientInterface observer) throws RemoteException;

    void removeBarrel(int barrelID) throws RemoteException;
}
