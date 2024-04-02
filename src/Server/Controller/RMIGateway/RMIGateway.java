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
    public int currentBarrel = 0;
    // TODO needs to be synchronized
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

    public boolean searchQuery(String query) throws RemoteException {
        // TODO If a barrel goes down, and the client sends a request, it might be
        // redirected to the crashed barrel and crash the client
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Got query: " + query);
        BarrelTimestamp barrel = getAvailableBarrel();
        // TODO maybe do something better if no barrel is available?
        if (barrel == null)
            return false;

        long startTime = System.currentTimeMillis();
        barrel.getRemoteBarrel().sayHi(query);
        barrel.setAvgResponseTime(System.currentTimeMillis() - startTime);
        return true;
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

    private synchronized BarrelTimestamp getAvailableBarrel() {
        for (BarrelTimestamp timedBarrel : timedBarrels)
            if (timedBarrel.getTimestamp() < System.currentTimeMillis() - BarrelPinger.PING_INTERVAL * 2)
                timedBarrels.remove(timedBarrel);

        if (timedBarrels.size() == 0) {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "No barrels available");
            return null;
        }

        currentBarrel = (currentBarrel + 1) % timedBarrels.size();
        return timedBarrels.get(currentBarrel);
    }
}