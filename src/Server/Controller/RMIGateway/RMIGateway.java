package Server.Controller.RMIGateway;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import Logger.LogUtil;
import ReliableMulticast.Receiver.ReceiverWorker;
import Server.IndexStorageBarrel.IndexStorageBarrel;

/**
 * Server.Controller.RMIGateway
 */
public class RMIGateway extends UnicastRemoteObject implements RMIGatewayInterface {
    int currentBarrel = 0;
    ArrayList<BarrelTimestamp> timedBarrels = new ArrayList<>();

    public static void main(String[] args) {
        try {
            System.out.println("Starting RMI Gateway...");
            RMIGateway rmiGateway = new RMIGateway();
            Registry registry = LocateRegistry.createRegistry(6001);
            registry.rebind("rmigateway", rmiGateway);
            System.out.println("RMI Gateway ready.");
        } catch (RemoteException re) {
            LogUtil.logError(LogUtil.ANSI_WHITE, RMIGateway.class, re);
        }
    }

    RMIGateway() throws RemoteException {
        super();
    }

    public void searchQuery(String query) throws RemoteException {
        getAvailableBarrel(query);

        // Pede ao barrel aqui: barrel.qqlcoisa();
        System.out.println("Got query: " + query);
    }

    private IndexStorageBarrel getAvailableBarrel(String query) {
        for (BarrelTimestamp timedBarrel : timedBarrels)
            if (timedBarrel.getTimestamp() < System.currentTimeMillis() - 10000)
                timedBarrels.remove(timedBarrel);

        if (timedBarrels.size() == 0) {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "No barrels available");
            return null;
        }

        currentBarrel = (currentBarrel + 1) % timedBarrels.size();
        return timedBarrels.get(currentBarrel).getBarrel();
    }

    public void sendMessage(String message) throws RemoteException {
        System.out.println("Got message: " + message);
    }

    public void addBarrel(BarrelTimestamp timedBarrel) {
        timedBarrels.add(timedBarrel);
    }
}