package Server.Controller.RMIGateway;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.Collectors;

import Logger.LogUtil;
import Server.IndexStorageBarrel.IndexStorageBarrel;
import Server.IndexStorageBarrel.IndexStorageBarrelInterface;
import Server.IndexStorageBarrel.Operations.BarrelPinger;
import Client.RMIClientInterface;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayInterface {
    public static final int PORT = 5999;
    public static final String REMOTE_REFERENCE_NAME = "rmigateway";
    private int currentBarrel = 0;
    private ArrayList<BarrelTimestamp> timedBarrels = new ArrayList<>();
    private final Map<String, Integer> searchQueries = new HashMap<>();
    private final List<RMIClientInterface> observers = new ArrayList<>();
    private String mostSearched = "";
    private String barrelsStatus = "";

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

    public void addObserver(RMIClientInterface observer) throws RemoteException {
        // Check if the observer is already in the list
        for (RMIClientInterface o : observers)
            if (o.equals(observer))
                return;
        observers.add(observer);
    }

    public void removeObserver(RMIClientInterface observer) throws RemoteException {
        // Check if the observer is in the list
        for (RMIClientInterface o : observers)
            if (o.equals(observer))
                observers.remove(observer);
    }

    private void notifyObservers() throws RemoteException {
        for (RMIClientInterface observer : observers) {
            if (observer.isOnMostSearchedPage())
                observer.updateMostSearched();
            if (observer.isOnBarrelStatusPage())
                observer.updateBarrelStatus();
        }
    }

    public boolean searchQuery(String query) throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Got query: " + query);
        BarrelTimestamp barrel = getAvailableBarrel();
        if (barrel == null)
            return false;

        long startTime = System.currentTimeMillis();
        barrel.getRemoteBarrel().sayHi(query);
        barrel.setAvgResponseTime(System.currentTimeMillis() - startTime);

        // Update the count of the search query
        searchQueries.put(query, searchQueries.getOrDefault(query, 0) + 1);

        // Check if the new query changes the top 10 most searched queries
        String oldMostSearched = mostSearched();
        updateMostSearched();
        String newMostSearched = mostSearched();

        if (!oldMostSearched.equals(newMostSearched)) {
            // Notify observers
            notifyObservers();
        }

        return true;
    }

    public void receivePing(int barrelID, long timestamp) throws RemoteException, NotBoundException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class,
                "Received ping from barrel " + barrelID);
        for (BarrelTimestamp timedBarrel : timedBarrels)
            if (barrelID == timedBarrel.getBarrelID()) {
                timedBarrel.setTimestamp(timestamp);
                return;
            }

        IndexStorageBarrelInterface remoteBarrel = (IndexStorageBarrelInterface) LocateRegistry
                .getRegistry(IndexStorageBarrel.STARTING_PORT + barrelID)
                .lookup(IndexStorageBarrel.REMOTE_REFERENCE_NAME + barrelID);

        if (remoteBarrel != null)
            timedBarrels.add(new BarrelTimestamp(remoteBarrel, timestamp, barrelID));

        // Check if the status of any barrel changes
        String oldBarrelsStatus = barrelsStatus();
        updateBarrelsStatus();
        String newBarrelsStatus = barrelsStatus();

        if (!oldBarrelsStatus.equals(newBarrelsStatus)) {
            // Notify observers
            notifyObservers();
        }
    }

    public void removeBarrel(int barrelID) throws RemoteException {
        timedBarrels.removeIf(timedBarrel -> timedBarrel.getBarrelID() == barrelID);
    }

    private synchronized BarrelTimestamp getAvailableBarrel() {
        timedBarrels.removeIf(timedBarrel -> timedBarrel.getTimestamp() < System.currentTimeMillis()
                - BarrelPinger.PING_INTERVAL * 2L);

        if (timedBarrels.isEmpty()) {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "No barrels available");
            return null;
        }

        boolean isAlive = false;
        while (!isAlive) {
            currentBarrel = (currentBarrel + 1) % timedBarrels.size();
            isAlive = isAlive(timedBarrels.get(currentBarrel));
            if (!isAlive)
                timedBarrels.remove(currentBarrel);
            if (timedBarrels.size() == 0)
                break;
        }
        return timedBarrels.get(currentBarrel);
    }

    private boolean isAlive(BarrelTimestamp barrelTimestamp) {
        try {
            barrelTimestamp.getRemoteBarrel().receivePing();
            return true;
        } catch (RemoteException re) {
            return false;
        }
    }

    public void updateMostSearched() {
        // Sort the searchQueries map by value in descending order
        List<Map.Entry<String, Integer>> sortedSearchQueries = new ArrayList<>(searchQueries.entrySet());
        sortedSearchQueries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Get the top 10 most searched queries
        List<String> topQueries = sortedSearchQueries.stream()
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Update the mostSearched variable
        mostSearched = String.join(", ", topQueries);
    }

    public void updateBarrelsStatus() {
        // Iterate over the timedBarrels list and check the status of each barrel
        StringBuilder status = new StringBuilder();
        for (BarrelTimestamp barrel : timedBarrels) {
            status.append("Barrel ID: ")
                    .append(barrel.getBarrelID())
                    .append(", Status: ")
                    .append(barrel.getTimestamp() < System.currentTimeMillis() - BarrelPinger.PING_INTERVAL * 2L
                            ? "Down"
                            : "Up")
                    .append(", Response Time: ")
                    .append(barrel.getAvgResponseTime())
                    .append(" ms\n");
        }

        // Update the barrelsStatus variable
        barrelsStatus = status.toString();
    }

    public String mostSearched() {
        return mostSearched;
    }

    public String barrelsStatus() {
        return barrelsStatus;
    }
}