package URLQueue;

import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface URLQueueInterface extends Remote {
    public void enqueueURL(URL url, int downloaderID) throws RemoteException;
    public URL dequeueURL(int downloaderID) throws RemoteException;
}
