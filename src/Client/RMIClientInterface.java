package Client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientInterface extends Remote {
    void updateMostSearched() throws RemoteException;
    void updateBarrelStatus() throws RemoteException;
    boolean isOnMostSearchedPage() throws RemoteException;
    boolean isOnBarrelStatusPage() throws RemoteException;
}
