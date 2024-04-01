package Server.Controller.RMIGateway;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGatewayInterface extends Remote {
    public void searchQuery(String query) throws RemoteException;

    public void receivePing(int barrelID, long timestamp) throws AccessException, RemoteException, NotBoundException;
}
