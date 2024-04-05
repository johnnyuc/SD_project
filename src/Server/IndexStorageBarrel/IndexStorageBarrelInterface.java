package Server.IndexStorageBarrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IndexStorageBarrelInterface extends Remote {
    public List<String> searchQuery(String query, int pageNumber) throws RemoteException;

    public void receivePing() throws RemoteException;
}
