package Server.IndexStorageBarrel;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IndexStorageBarrelInterface extends Remote {
    public void sayHi(String query) throws RemoteException;

    public void receivePing() throws RemoteException;
}
