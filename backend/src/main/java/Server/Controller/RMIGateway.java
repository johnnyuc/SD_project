package Server.Controller;

// Package imports
import Server.URLQueue.URLQueue;
import Server.Controller.Objects.Stats;
import Server.IndexStorageBarrel.IndexStorageBarrel;
import Server.IndexStorageBarrel.Operations.BarrelPinger;
import Server.IndexStorageBarrel.IndexStorageBarrelInterface;
import Server.IndexStorageBarrel.Objects.SearchData;
// Logging imports
import Logger.LogUtil;

// General imports
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.rmi.Naming;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

// Exception imports
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

/**
 * The RMIGateway class represents the RMI gateway for the search engine system.
 * It handles the communication between the client and the server.
 */
public class RMIGateway extends UnicastRemoteObject implements RMIGatewayInterface {
    /**
     * The port number for the RMI gateway.
     */
    public static final int PORT = 5999;
    /**
     * The remote reference name for the RMI gateway.
     */
    public static final String REMOTE_REFERENCE_NAME = "rmigateway";
    /**
     * The starting port number for the barrels.
     */
    private int currentBarrel = 0;
    /**
     * The list of timed barrels.
     */
    private final ArrayList<BarrelTimestamp> timedBarrels = new ArrayList<>();
    /**
     * The search queries and their counts.
     */
    private final Map<String, Integer> searchQueries = new HashMap<>();
    /**
     * The address of the URL queue.
     */
    private String queueAddress;

    /**
     * The main method that starts the RMI gateway.
     * 
     * @param args The command line arguments.
     */
    public static void main(String[] args) {

        try {
            LogUtil.logInfo(LogUtil.ANSI_YELLOW, RMIGateway.class, "Starting RMI Gateway...");
            RMIGateway rmiGateway = new RMIGateway(args);
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind(REMOTE_REFERENCE_NAME, rmiGateway);
            LogUtil.logInfo(LogUtil.ANSI_GREEN, RMIGateway.class, "RMI Gateway ready.");
        } catch (RemoteException re) {
            LogUtil.logError(LogUtil.ANSI_RED, RMIGateway.class, re);
        }
    }

    /**
     * Constructs a new RMIGateway object.
     * 
     * @param args The command line arguments.
     * @throws RemoteException if a remote error occurs.
     */
    public RMIGateway(String[] args) throws RemoteException {
        super();
        if (!processArgs(args))
            System.exit(1);
    }

    /**
     * Searches for the given query and returns the search results.
     * 
     * @param query      The search query.
     * @param pageNumber The page number of the search results.
     * @return The search results.
     * @throws RemoteException if a remote error occurs.
     */
    public List<SearchData> searchQuery(String query, int pageNumber) throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_BLUE, RMIGateway.class, "Got query: " + query);

        if (isValidURL(query)) {
            // The query is a URL so send it to the Barrel
            priorityEnqueueURL(query);
            return Collections.singletonList(new SearchData(
                    "", "URL Indexed.", "", 0, SearchData.EXCEPTION));
        }

        int barrel = getAvailableBarrel();
        if (barrel == -1)
            return Collections.singletonList(new SearchData(
                    "", "No barrels available.", "", 0, SearchData.EXCEPTION));

        LogUtil.logInfo(LogUtil.ANSI_BLUE, RMIGateway.class, "Got barrel: " + barrel);

        long startTime = System.currentTimeMillis();
        // The query is a URL so send it to the URL Queue
        List<SearchData> results = timedBarrels.get(currentBarrel).getRemoteBarrel().searchQuery(query, pageNumber);

        timedBarrels.get(barrel).setAvgResponseTime(System.currentTimeMillis() - startTime);

        // Update the count of the search query
        searchQueries.put(query, searchQueries.getOrDefault(query, 0) + 1);

        // Send the statistics to the web application
        sendStats();

        // Convert the SearchData objects to String and return the search results
        return results;
    }

    /**
     * Retrieves the websites that are linking to the target URL.
     * 
     * @param targetUrl  The target URL.
     * @param pageNumber The page number of the search results.
     * @return The websites linking to the target URL.
     * @throws RemoteException if a remote error occurs.
     */
    public List<SearchData> getWebsitesLinkingTo(String targetUrl, int pageNumber) throws RemoteException {
        int currentBarrel = getAvailableBarrel();
        if (currentBarrel == -1)
            return Collections.singletonList(new SearchData(
                    "", "No barrels available.", "", 0, SearchData.EXCEPTION));

        long startTime = System.currentTimeMillis();
        List<SearchData> results = timedBarrels.get(currentBarrel).getRemoteBarrel()
                .getWebsitesLinkingTo(targetUrl, pageNumber);

        timedBarrels.get(currentBarrel).setAvgResponseTime(System.currentTimeMillis() - startTime);

        // Send the statistics to the web application
        sendStats();

        return results;
    }

    /**
     * Checks if the given URL is valid.
     * 
     * @param url The URL to check.
     * @return true if the URL is valid, false otherwise.
     */
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

    /**
     * Processes the command line arguments.
     *
     * @param args The command line arguments.
     */
    private boolean processArgs(String[] args) {
        if (args.length != 2) {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Wrong number of arguments: expected -qadd <queue address>");
            return false;
        }
        // Parse the arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-qadd")) {
                queueAddress = args[++i];
            } else {
                LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                        "Unexpected argument: " + args[i]);
                return false;
            }
        }
        return true;
    }

    /**
     * Receives a ping from a barrel.
     * 
     * @param barrelID The ID of the barrel.
     * @param barrelIP The IP address of the barrel.
     * @throws RemoteException       if a remote error occurs.
     * @throws NotBoundException     if the barrel is not bound.
     * @throws MalformedURLException if the URL is malformed.
     */
    public void receivePing(int barrelID, String barrelIP)
            throws RemoteException, NotBoundException, MalformedURLException {

        LogUtil.logInfo(LogUtil.ANSI_BLUE, RMIGateway.class,
                "Received ping from barrel " + barrelID + " with IP " + barrelIP);

        for (BarrelTimestamp timedBarrel : timedBarrels) {
            if (timedBarrel.getBarrelID() == barrelID) {
                timedBarrel.setTimestamp(System.currentTimeMillis());
                return;
            }
        }

        IndexStorageBarrelInterface remoteBarrel = (IndexStorageBarrelInterface) Naming
                .lookup("rmi://" + barrelIP + ":"
                        + (IndexStorageBarrel.STARTING_PORT + barrelID) + "/"
                        + (IndexStorageBarrel.REMOTE_REFERENCE_NAME + barrelID));

        if (remoteBarrel != null) {
            timedBarrels.add(new BarrelTimestamp(remoteBarrel, System.currentTimeMillis(), barrelID));
            sendStats();
        }
    }

    /**
     * Removes a barrel from the list of timed barrels.
     * 
     * @param barrelID The ID of the barrel to remove.
     * @throws RemoteException if a remote error occurs.
     */
    public void removeBarrel(int barrelID) throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_BLUE, RMIGateway.class, "Removing barrel " + barrelID);
        timedBarrels.removeIf(timedBarrel -> timedBarrel.getBarrelID() == barrelID);
        // Send the statistics to the web application
        sendStats();
    }

    /**
     * Gets an available barrel for processing.
     * 
     * @return The index of the available barrel, or -1 if no barrels are available.
     */
    private synchronized int getAvailableBarrel() {
        timedBarrels.removeIf(timedBarrel -> timedBarrel.getTimestamp() < System.currentTimeMillis()
                - BarrelPinger.PING_INTERVAL * 2L);

        if (timedBarrels.isEmpty()) {
            LogUtil.logInfo(LogUtil.ANSI_BLUE, RMIGateway.class, "No barrels available");
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

    public int getActiveBarrels() throws RemoteException {
        return timedBarrels.size();
    }

    /**
     * Retrieves the most searched queries.
     * 
     * @return The most searched queries.
     * @throws RemoteException if a remote error occurs.
     */
    public List<String> mostSearched() throws RemoteException {
        int currentBarrel = getAvailableBarrel();
        if (currentBarrel == -1)
            return Collections.singletonList("No barrels available.");

        long startTime = System.currentTimeMillis();
        List<String> results = timedBarrels.get(currentBarrel).getRemoteBarrel().getTopSearches();
        timedBarrels.get(currentBarrel).setAvgResponseTime(System.currentTimeMillis() - startTime);

        return results;
    }

    /**
     * Checks if a barrel is alive.
     * 
     * @param barrelTimestamp The barrel to check.
     * @return true if the barrel is alive, false otherwise.
     */
    private boolean isAlive(BarrelTimestamp barrelTimestamp) {
        try {
            barrelTimestamp.getRemoteBarrel().receivePing();
            return true;
        } catch (RemoteException re) {
            return false;
        }
    }

    /**
     * Retrieves the status of the barrels.
     * 
     * @return The status of the barrels.
     * @throws RemoteException if a remote error occurs.
     */
    public List<String> barrelsStatus() throws RemoteException {
        List<String> status = new ArrayList<>();
        for (BarrelTimestamp barrel : timedBarrels) {
            status.add("Barrel %d : %dms".formatted(barrel.getBarrelID(), barrel.getAvgResponseTime()));
        }
        return status;
    }

    /**
     * Enqueues a URL with priority.
     *
     * @param url The URL to enqueue.
     * @throws RemoteException if a remote error occurs.
     */
    public void priorityEnqueueURL(String url) throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_BLUE, RMIGateway.class, "Priority enqueueing URL: " + url);
        try (DatagramSocket aSocket = new DatagramSocket()) {
            byte[] url_bytes = url.getBytes();

            InetAddress aHost = InetAddress.getByName(queueAddress);
            DatagramPacket request = new DatagramPacket(url_bytes, url_bytes.length, aHost, URLQueue.UDP_PORT);

            aSocket.send(request);
        } catch (IOException e) {
            LogUtil.logError(LogUtil.ANSI_RED, RMIGateway.class, e);
        }
    }

    /**
     * Retrieves the stats of the system.
     * 
     * @return the stats of the system
     * @throws RemoteException
     */
    public Stats getStats() throws RemoteException {
        return new Stats(barrelsStatus(), mostSearched());
    }

    /**
     * Sends the statistics to the web application.
     * 
     * @throws RemoteException if a remote error occurs.
     */
    private void sendStats() throws RemoteException {
        String url = "http://localhost:8080/trigger-stats";
        String authToken = "someauthtokenidk";

        RestTemplate restTemplate = new RestTemplate();
        Stats stats = new Stats(barrelsStatus(), mostSearched());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authToken);

        HttpEntity<Stats> entity = new HttpEntity<>(stats, headers);

        // Send the request and get the response
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            // Print the response
            LogUtil.logInfo(LogUtil.ANSI_GREEN, RMIGateway.class, response.getBody());
        } catch (Exception e) {
            LogUtil.logInfo(LogUtil.ANSI_RED, RMIGateway.class, "Error sending stats to the web application.");
        }
    }
}