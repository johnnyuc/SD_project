package Server.Controller.RMIGateway;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import Logger.LogUtil;
import Server.IndexStorageBarrel.IndexStorageBarrel;
import Server.IndexStorageBarrel.IndexStorageBarrelInterface;
import Server.IndexStorageBarrel.Operations.BarrelPinger;
import Server.URLQueue.URLQueue;
import Server.URLQueue.URLQueueInterface;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayInterface {
    public static final int PORT = 5999;
    public static final String REMOTE_REFERENCE_NAME = "rmigateway";
    private int currentBarrel = 0;
    private final ArrayList<BarrelTimestamp> timedBarrels = new ArrayList<>();
    private final Map<String, Integer> searchQueries = new HashMap<>();
    private String mostSearched = "";
    private String barrelsStatus = "";
    private URLQueueInterface urlQueue;
    private String queueAddress;

    public static void main(String[] args) {

        try {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Starting RMI Gateway...");
            RMIGateway rmiGateway = new RMIGateway(args);
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind(REMOTE_REFERENCE_NAME, rmiGateway);
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "RMI Gateway ready.");
        } catch (RemoteException re) {
            LogUtil.logError(LogUtil.ANSI_WHITE, RMIGateway.class, re);
        }
    }

    public RMIGateway(String[] args) throws RemoteException {
        super();
        if (!processArgs(args))
            return;
        try {
            urlQueue = (URLQueueInterface) Naming
                    .lookup("rmi://" + queueAddress + ":" + URLQueue.PORT + "/" + URLQueue.REMOTE_REFERENCE_NAME);
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            LogUtil.logError(LogUtil.ANSI_RED, RMIGateway.class, e);
        }

    }

    public List<String> searchQuery(String query, int pageNumber) throws RemoteException, MalformedURLException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Got query: " + query);

        if (isValidURL(query)) {
            // The query is a URL so send it to the Barrel
            urlQueue.priorityEnqueueURL(URI.create(query).toURL());
            return Collections.singletonList("URL Indexed.");
        }

        int barrel = getAvailableBarrel();
        if (currentBarrel == -1)
            return Collections.singletonList("No barrels available.");

        long startTime = System.currentTimeMillis();
        // The query is a URL so send it to the URL Queue
        List<String> results = timedBarrels.get(currentBarrel).getRemoteBarrel().searchQuery(query, pageNumber);

        timedBarrels.get(barrel).setAvgResponseTime(System.currentTimeMillis() - startTime);

        // Update the count of the search query
        searchQueries.put(query, searchQueries.getOrDefault(query, 0) + 1);

        // Convert the SearchData objects to String and return the search results
        return results;
    }

    public List<String> getWebsitesLinkingTo(String targetUrl, int pageNumber) throws RemoteException {
        int currentBarrel = getAvailableBarrel();
        if (currentBarrel == -1)
            return Collections.singletonList("No barrels available.");

        long startTime = System.currentTimeMillis();
        List<String> results = timedBarrels.get(currentBarrel).getRemoteBarrel()
                .getWebsitesLinkingTo(targetUrl, pageNumber);

        timedBarrels.get(currentBarrel).setAvgResponseTime(System.currentTimeMillis() - startTime);

        return results;
    }

    public static boolean isValidURL(String url) {
        // Regex to check valid URL
        String regex = "((http|https)://)(www.)?"
                + "[a-zA-Z0-9@:%._+~#?&/=]"
                + "{2,256}\\.[a-z]"
                + "{2,6}\\b([-a-zA-Z0-9@:%"
                + "._+~#?&/=]*)";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the string is empty return false
        if (url == null)
            return false;

        // Find match between given string and regular expression using
        // Pattern.matcher()
        Matcher m = p.matcher(url);

        // Return if the string matched the ReGex
        return m.matches();
    }

    private boolean processArgs(String[] args) {
        if (args.length != 2) {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Wrong number of arguments: expected -qadd <queue address>");
            return false;
        }
        // Parse the arguments
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-qadd")) {
                    queueAddress = args[++i];
                } else {
                    LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                            "Unexpected argument: " + args[i]);
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Wrong type of argument: expected int for barrel id and port number");
            return false;
        }
        return true;
    }

    public void receivePing(int barrelID, String barrelIP)
            throws RemoteException, NotBoundException, MalformedURLException {

        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class,
                "Received ping from barrel " + barrelID + " with IP " + barrelIP);

        for (int i = 0; i < timedBarrels.size(); i++) {
            if (timedBarrels.get(i).getBarrelID() == barrelID) {
                timedBarrels.get(i).setTimestamp(System.currentTimeMillis());
                return;
            }
        }

        IndexStorageBarrelInterface remoteBarrel = (IndexStorageBarrelInterface) Naming
                .lookup("rmi://" + barrelIP + ":"
                        + (IndexStorageBarrel.STARTING_PORT + barrelID) + "/"
                        + (IndexStorageBarrel.REMOTE_REFERENCE_NAME + barrelID));

        System.out.println(remoteBarrel);

        if (remoteBarrel != null)
            timedBarrels.add(new BarrelTimestamp(remoteBarrel, System.currentTimeMillis(), barrelID));
    }

    public void removeBarrel(int barrelID) throws RemoteException {
        timedBarrels.removeIf(timedBarrel -> timedBarrel.getBarrelID() == barrelID);
    }

    private synchronized int getAvailableBarrel() {
        for (BarrelTimestamp timedBarrel : timedBarrels) {
            if (timedBarrel.getTimestamp() < System.currentTimeMillis() - BarrelPinger.PING_INTERVAL * 2)
                timedBarrels.remove(timedBarrel);
        }

        if (timedBarrels.isEmpty()) {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "No barrels available");
            return -1;
        }

        boolean isAlive = false;
        while (!isAlive) {
            currentBarrel = (currentBarrel + 1) % timedBarrels.size();
            isAlive = isAlive(timedBarrels.get(currentBarrel));
            if (!isAlive)
                timedBarrels.remove(currentBarrel);
            if (timedBarrels.isEmpty())
                break;
        }
        return currentBarrel;
    }

    private boolean isAlive(BarrelTimestamp barrelTimestamp) {
        try {
            barrelTimestamp.getRemoteBarrel().receivePing();
            return true;
        } catch (RemoteException re) {
            return false;
        }
    }

    public List<String> mostSearched() throws RemoteException {
        int currentBarrel = getAvailableBarrel();
        if (currentBarrel == -1)
            return Collections.singletonList("No barrels available.");

        long startTime = System.currentTimeMillis();
        List<String> results = timedBarrels.get(currentBarrel).getRemoteBarrel().getTopSearches();
        timedBarrels.get(currentBarrel).setAvgResponseTime(System.currentTimeMillis() - startTime);
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Most searched queries: " + results);
        return results;
    }

    public String barrelsStatus() throws RemoteException {
        String str = "";
        for (BarrelTimestamp barrel : timedBarrels) {
            str += "Barrel " + barrel.getBarrelID() + " : " + barrel.getAvgResponseTime() + "ms\n";
        }
        return str;
    }
}