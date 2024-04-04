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
import java.util.List;

import Logger.LogUtil;

public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface {
    private Connection conn;
    private BarrelPopulate barrelPopulate;
    private BarrelRetriever barrelRetriever;
    private BarrelSetup barrelSetup;
    private String dbPath;

    private int barrelID;

    private String downloaderMcastGroupAddress;
    private int downloaderMcastPort;

    private String syncMcastGroupAddress;
    private int syncMcastPort;

    private String mcastAddress;
    private String barrelAddress;
    private String gatewayAddress;

    public static final int STARTING_PORT = 6000;
    public static final String REMOTE_REFERENCE_NAME = "indexstoragebarrel";

    public static void main(String[] args) {
        try {
            new IndexStorageBarrel(args);
        } catch (RemoteException re) {
            LogUtil.logError(LogUtil.ANSI_RED, IndexStorageBarrel.class, re);
        }
    }

    public IndexStorageBarrel(String[] args) throws RemoteException {
        super();
        LogUtil.logInfo(LogUtil.ANSI_WHITE, RMIGateway.class, "Starting Index Storage Barrel...");
        if (!processArgs(args))
            return;

        startRMI();
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:data/" + dbPath + ".db");
            BarrelSetup.databaseIntegrity(conn); // Check database integrity
            this.barrelPopulate = new BarrelPopulate(conn);
            this.barrelRetriever = new BarrelRetriever(conn);
            // Barrel receiver
            Class<?>[] receiverIgnoredClasses = { BarrelSync.class };
            new BarrelReceiver(barrelPopulate,
                    new ReliableMulticast(mcastAddress, downloaderMcastGroupAddress, downloaderMcastPort,
                            BarrelReceiver.class, receiverIgnoredClasses));
            // Barrel sync
            Class<?>[] syncIgnoredClasses = { DownloaderWorker.class };
            new BarrelSync(barrelPopulate, barrelRetriever,
                    new ReliableMulticast(mcastAddress, syncMcastGroupAddress, syncMcastPort,
                            BarrelSync.class, syncIgnoredClasses));

            new BarrelPinger(barrelID, barrelAddress, gatewayAddress);
            LogUtil.logInfo(LogUtil.ANSI_WHITE, IndexStorageBarrel.class, "Index Storage Barrel ready.");
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, IndexStorageBarrel.class, e);
        }
    }

    private boolean processArgs(String[] args) {
        // TODO: Se este erro acontecer, as threads que o barrel criou nao vao abaixo
        if (args.length != 18) {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Wrong number of arguments: expected -id <barrel id> -db <database path> "
                            + "-dmcast <downloader multicast group address> -dport <downloader port number> "
                            + "-smcast <sync multicast group address> -sport <sync port number> "
                            + "-mcastadd <multicast interface address> -badd <barrel interface address>"
                            + "-gadd <gateway interface address>");
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

    private void startRMI() {
        try {
            Registry registry;
            registry = LocateRegistry.createRegistry(STARTING_PORT + barrelID);
            registry.rebind(REMOTE_REFERENCE_NAME + barrelID, this);
        } catch (RemoteException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, IndexStorageBarrel.class, e);
        }
    }

    public void insertData(CrawlData crawlData) throws SQLException {
        barrelPopulate.insertCrawlData(crawlData);
    }

    public List<CrawlData> retrieveObject() {
        return barrelRetriever.retrieveObject();
    }

    public List<SearchData> retrieveAndRankData(String query) {
        return barrelRetriever.retrieveAndRankData(query);
    }

    public void sayHi(String query) throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, IndexStorageBarrel.class, "Hi from client with query " + query);
    }

    public void receivePing() throws RemoteException {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, IndexStorageBarrel.class, "Received ping from RMI Gateway.");
    }

    public int getBarrelID() {
        return barrelID;
    }
}