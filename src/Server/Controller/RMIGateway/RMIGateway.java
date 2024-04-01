package Server.Controller.RMIGateway;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import Logger.LogUtil;
import Server.IndexStorageBarrel.IndexStorageBarrel;
import Server.IndexStorageBarrel.IndexStorageBarrelInterface;
import Server.IndexStorageBarrel.Operations.BarrelPinger;

/**
 * Server.Controller.RMIGateway
 */
public class RMIGateway extends UnicastRemoteObject implements RMIGatewayInterface {
    public static final int PORT = 5999;
    public static final String REMOTE_REFERENCE_NAME = "rmigateway";
    int currentBarrel = 0;
    ArrayList<BarrelTimestamp> timedBarrels = new ArrayList<>();

    public static void main(String[] args) {
        try {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Starting RMI Gateway...");
            RMIGateway rmiGateway = new RMIGateway();
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind(REMOTE_REFERENCE_NAME, rmiGateway);
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "RMI Gateway ready.");
        } catch (RemoteException re) {
            LogUtil.logError(LogUtil.ANSI_WHITE, RMIGateway.class, re);
        }
    }

    RMIGateway() throws RemoteException {
        super();
    }

    public void searchQuery(String query) throws RemoteException {
        // TODO If a barrel goes down, and the client sends a request, it might be
        // redirected to the crashed barrel and crash the client
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Got query: " + query);
        IndexStorageBarrelInterface remoteBarrel = getAvailableBarrel();
        if (remoteBarrel == null)
            return;

        remoteBarrel.sayHi(query);

    }

    public void receivePing(int barrelID, long timestamp) throws AccessException, RemoteException, NotBoundException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class,
                "Received ping from barrel " + barrelID);
        // Update the timestamp of the barrel
        for (BarrelTimestamp timedBarrel : timedBarrels)
            if (barrelID == timedBarrel.getBarrelID()) {
                timedBarrel.setTimestamp(timestamp);
                return;
            }

        // If no barrel was found, add it
        IndexStorageBarrelInterface remoteBarrel = (IndexStorageBarrelInterface) LocateRegistry
                .getRegistry(IndexStorageBarrel.STARTING_PORT + barrelID)
                .lookup(IndexStorageBarrel.REMOTE_REFERENCE_NAME + barrelID);

        if (remoteBarrel != null)
            timedBarrels.add(new BarrelTimestamp(remoteBarrel, timestamp, barrelID));

    }

    private IndexStorageBarrelInterface getAvailableBarrel() {
        for (BarrelTimestamp timedBarrel : timedBarrels)
            if (timedBarrel.getTimestamp() < System.currentTimeMillis() - BarrelPinger.PING_INTERVAL * 2)
                timedBarrels.remove(timedBarrel);

        if (timedBarrels.size() == 0) {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "No barrels available");
            return null;
        }

        currentBarrel = (currentBarrel + 1) % timedBarrels.size();
        return timedBarrels.get(currentBarrel).getRemoteBarrel();
    }
}