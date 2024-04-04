package Server.URLQueue;

import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface URLQueueInterface extends Remote {
    void enqueueURL(URL url, int downloaderID) throws RemoteException;

    URL dequeueURL(int downloaderID) throws RemoteException;

    public void priorityEnqueueURL(URL url) throws RemoteException;
}
