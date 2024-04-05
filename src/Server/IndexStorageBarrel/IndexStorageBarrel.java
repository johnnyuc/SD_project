package Server.IndexStorageBarrel;

import ReliableMulticast.ReliableMulticast;
import ReliableMulticast.Objects.CrawlData;
import Server.Controller.RMIGateway.RMIGateway;
import Server.Downloader.DownloaderWorker;
import Server.IndexStorageBarrel.Objects.SearchData;
import Server.IndexStorageBarrel.Operations.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import Logger.LogUtil;

/**
 * The IndexStorageBarrel class represents a storage barrel for indexing and
 * storing crawl data.
 * It implements the IndexStorageBarrelInterface interface and extends the
 * UnicastRemoteObject class
 * to enable remote method invocation (RMI) functionality.
 * 
 * The IndexStorageBarrel class provides methods for inserting crawl data,
 * retrieving crawl data,
 * searching for data based on a query, and performing other operations related
 * to indexing and storage.
 * 
 * This class also includes a main method for starting the IndexStorageBarrel as
 * a standalone application.
 */
public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface {
    private Connection conn;
    private BarrelPinger barrelPinger;
    private BarrelPopulate barrelPopulate;
    private BarrelReceiver barrelReceiver;
    private BarrelRetriever barrelRetriever;
    private BarrelSync barrelSync;
    private String dbPath;

    private int barrelID;

    private String downloaderMcastGroupAddress;
    private int downloaderMcastPort;

    private String syncMcastGroupAddress;
    private int syncMcastPort;

    private String mcastAddress;
    private String barrelAddress;

    private String gatewayAddress;

    private boolean tfIdfSort = false;

    private final CountDownLatch latch;

    public static final int STARTING_PORT = 6000;
    public static final String REMOTE_REFERENCE_NAME = "indexstoragebarrel";

    public static void main(String[] args) {
        try {
            new IndexStorageBarrel(args);
        } catch (RemoteException re) {
            LogUtil.logError(LogUtil.ANSI_RED, IndexStorageBarrel.class, re);
        }
    }

    /**
     * Constructs a new instance of the IndexStorageBarrel class.
     *
     * @param args the command-line arguments passed to the program
     * @throws RemoteException if a remote exception occurs
     */
    public IndexStorageBarrel(String[] args) throws RemoteException {
        super();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        LogUtil.logInfo(LogUtil.ANSI_YELLOW, RMIGateway.class, "Starting Index Storage Barrel...");
        if (!processArgs(args))
            System.exit(1);

        this.latch = new CountDownLatch(1);

        startRMI();
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:data/" + dbPath + ".db");
            BarrelSetup.databaseIntegrity(conn, dbPath); // Check database integrity
            this.barrelPopulate = new BarrelPopulate(conn);
            this.barrelRetriever = new BarrelRetriever(conn);
            this.barrelPinger = new BarrelPinger(this);

            // Barrel receiver
            Class<?>[] receiverIgnoredClasses = { BarrelSync.class };
            this.barrelReceiver = new BarrelReceiver(this,
                    new ReliableMulticast(mcastAddress, downloaderMcastGroupAddress,
                            downloaderMcastPort, BarrelReceiver.class, receiverIgnoredClasses));
            // Barrel sync
            Class<?>[] syncIgnoredClasses = { DownloaderWorker.class };
            this.barrelSync = new BarrelSync(this,
                    new ReliableMulticast(mcastAddress, syncMcastGroupAddress, syncMcastPort,
                            BarrelSync.class, syncIgnoredClasses));

            LogUtil.logInfo(LogUtil.ANSI_GREEN, IndexStorageBarrel.class, "Index Storage Barrel ready.");
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, IndexStorageBarrel.class, e);
            stop();
        }
    }

    /**
     * Processes the command line arguments and initializes the necessary variables.
     *
     * @param args the command line arguments
     * @return true if the arguments are valid and successfully processed, false
     *         otherwise
     */
    private boolean processArgs(String[] args) {
        if (args.length < 18) {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Wrong number of arguments: expected -id <barrel id> -db <database path> "
                            + "-dmcast <downloader multicast group address> -dport <downloader port number> "
                            + "-smcast <sync multicast group address> -sport <sync port number> "
                            + "-mcastadd <multicast interface address> -badd <barrel interface address> "
                            + "-gadd <gateway interface address>"
                            + "-s <optional for TF-IDF sort>");
            stop();
            return false;
        }
        // Parse the arguments
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-id" -> barrelID = Integer.parseInt(args[++i]);
                    case "-db" -> dbPath = args[++i];
                    case "-dmcast" -> downloaderMcastGroupAddress = args[++i];
                    case "-dport" -> downloaderMcastPort = Integer.parseInt(args[++i]);
                    case "-smcast" -> syncMcastGroupAddress = args[++i];
                    case "-sport" -> syncMcastPort = Integer.parseInt(args[++i]);
                    case "-mcastadd" -> mcastAddress = args[++i];
                    case "-badd" -> barrelAddress = args[++i];
                    case "-gadd" -> gatewayAddress = args[++i];
                    case "-s" -> tfIdfSort = true;
                    default -> {
                        LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                                "Unexpected argument: " + args[i]);
                        return false;
                    }
                }
            }
        } catch (NumberFormatException e) {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Wrong type of argument: expected int for barrel id and port number");
            return false;
        }
        return true;
    }

    /**
     * Starts the RMI server for the IndexStorageBarrel.
     * This method creates a registry and binds the remote reference of the barrel
     * to the registry.
     * If an exception occurs during the process, it logs the error using LogUtil.
     */
    private void startRMI() {
        try {
            Registry registry;
            registry = LocateRegistry.createRegistry(STARTING_PORT + barrelID);
            registry.rebind(REMOTE_REFERENCE_NAME + barrelID, this);
        } catch (RemoteException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, IndexStorageBarrel.class, e);
        }
    }

    /**
     * Inserts the given crawl data into the index storage barrel.
     *
     * @param crawlData the crawl data to be inserted
     * @throws SQLException if an error occurs while inserting the data
     */
    public void insertData(CrawlData crawlData) throws SQLException {
        barrelPopulate.insertCrawlData(crawlData);
    }

    /**
     * Retrieves the list of CrawlData objects from the barrel.
     *
     * @return The list of CrawlData objects retrieved from the barrel.
     */
    public List<CrawlData> retrieveObject() {
        return barrelRetriever.retrieveObject();
    }

    /**
     * Retrieves and ranks search data based on the given query and page number.
     *
     * @param query      the search query
     * @param pageNumber the page number of the search results
     * @return a list of SearchData objects representing the ranked search results
     */
    private List<SearchData> retrieveAndRankData(String query, int pageNumber, boolean tfIdfSort) {
        return tfIdfSort ? barrelRetriever.retrieveAndRankData(query, pageNumber, true)
                : barrelRetriever.retrieveAndRankData(query, pageNumber, false);
    }

    /**
     * Searches for a given query and returns a list of search results.
     *
     * @param query      The search query to be performed.
     * @param pageNumber The page number of the search results.
     * @return A list of search results as strings.
     * @throws RemoteException If a remote exception occurs during the search.
     */
    public List<String> searchQuery(String query, int pageNumber) throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, IndexStorageBarrel.class, "Received query:" + query);
        List<SearchData> searchData = retrieveAndRankData(query, pageNumber, tfIdfSort);

        // Tokenize query and send each keyword to increment searches
        String[] keywords = query.split(" ");
        for (String keyword : keywords) {
            LogUtil.logInfo(LogUtil.ANSI_WHITE, IndexStorageBarrel.class,
                    "Incrementing searches for keyword:" + keyword);
            barrelPopulate.incrementSearches(keyword);
        }

        List<String> results = new ArrayList<>();
        for (SearchData sD : searchData) {
            results.add(sD.title() + "\n" + sD.url() + "\nRef_count: " + sD.refCount() + " TF_IDF: " + sD.tfIdf());
        }

        return results;
    }

    /**
     * Get the top searches
     * 
     * @return the top searches
     * @throws RemoteException if a remote exception occurs
     */
    public List<String> getTopSearches() throws RemoteException {
        return barrelRetriever.getTopSearches();
    }

    /**
     * Get the websites linking to the target URL
     * 
     * @param targetUrl  the target URL
     * @param pageNumber the page number
     * @return the websites linking to the target URL
     * @throws RemoteException if a remote exception occurs
     */
    public List<String> getWebsitesLinkingTo(String targetUrl, int pageNumber) throws RemoteException {
        return barrelRetriever.getWebsitesLinkingTo(targetUrl, pageNumber);
    }

    /**
     * Receive a ping from the RMI Gateway
     * 
     * @throws RemoteException if a remote exception occurs
     */
    public void receivePing() throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, IndexStorageBarrel.class, "Received ping from RMI Gateway.");
    }

    /**
     * Stop the barrel
     */
    public void stop() {
        if (conn != null)
            try {
                conn.close();
            } catch (SQLException e) {
                LogUtil.logError(LogUtil.ANSI_RED, IndexStorageBarrel.class, e);
            }

        if (barrelPinger != null)
            barrelPinger.setRunning(false);
        if (barrelReceiver != null)
            barrelReceiver.setRunning(false);
        if (barrelSync != null)
            barrelSync.setRunning(false);
    }

    // Getters and Setters

    /**
     * Returns the BarrelPinger object associated with this IndexStorageBarrel.
     *
     * @return the BarrelPinger object associated with this IndexStorageBarrel
     */
    public BarrelPinger getBarrelPinger() {
        return barrelPinger;
    }

    /**
     * Returns the ID of the barrel.
     *
     * @return the ID of the barrel
     */
    public int getBarrelID() {
        return barrelID;
    }

    /**
     * Returns the BarrelPopulate object associated with this IndexStorageBarrel.
     *
     * @return the BarrelPopulate object associated with this IndexStorageBarrel
     */
    public BarrelPopulate getBarrelPopulate() {
        return barrelPopulate;
    }

    /**
     * Returns the BarrelRetriever object associated with this IndexStorageBarrel.
     *
     * @return the BarrelRetriever object
     */
    public BarrelRetriever getBarrelRetriever() {
        return barrelRetriever;
    }

    /**
     * Represents a synchronization object for the barrel storage.
     * This class is responsible for managing the synchronization of the barrel
     * storage operations.
     * 
     * @return the BarrelSync object
     */
    public BarrelSync getBarrelSync() {
        return barrelSync;
    }

    /**
     * Returns the address of the barrel.
     *
     * @return the address of the barrel
     */
    public String getBarrelAddress() {
        return barrelAddress;
    }

    /**
     * Returns the gateway address.
     *
     * @return the gateway address as a String.
     */
    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
