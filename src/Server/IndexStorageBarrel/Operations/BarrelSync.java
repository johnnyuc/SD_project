package Server.IndexStorageBarrel.Operations;

// Package imports
import ReliableMulticast.ReliableMulticast;
import Server.IndexStorageBarrel.Tools.SyncData;
import Server.IndexStorageBarrel.Tools.SyncRequest;
import Server.IndexStorageBarrel.IndexStorageBarrel;

// Logger imports
import Logger.LogUtil;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

// Exception imports
import java.rmi.RemoteException;

/**
 * The BarrelSync class implements the Runnable interface and is responsible for
 * synchronizing data between IndexStorageBarrel and ReliableMulticast.
 * It starts a new thread to perform the synchronization process.
 */
public class BarrelSync implements Runnable {
    /**
     * The IndexStorageBarrel object used to perform database operations.
     */
    private final IndexStorageBarrel barrel;
    /**
     * The ReliableMulticast object used to send and receive data.
     */
    private final ReliableMulticast reliableMulticast;
    /**
     * A boolean value indicating whether the synchronization process is running.
     */
    private boolean running = true;

    /**
     * Constructor for BarrelSync
     * 
     * @param barrel            IndexStorageBarrel object to perform database
     *                          operations
     * @param reliableMulticast ReliableMulticast object to send and receive data
     */
    public BarrelSync(IndexStorageBarrel barrel, ReliableMulticast reliableMulticast) {
        this.barrel = barrel;
        this.reliableMulticast = reliableMulticast;
        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        new Thread(this, "Barrel Sync").start();
    }

    @Override
    public void run() {
        reliableMulticast.startReceiving();
        try {
            // The nr of active barrels will be zero since this one wont have pinged the
            // gateway yet
            if (barrel.getBarrelPinger().getActiveBarrels() == 0)
                barrel.getLatch().countDown();
            else
                reliableMulticast.send(getSyncRequest());
        } catch (RemoteException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelSync.class, e);
        }

        while (running) {
            Object data = reliableMulticast.getData();
            if (data == null)
                running = false;
            else if (data.getClass() == SyncRequest.class)
                reliableMulticast.send(getSyncData((SyncRequest) data));
            else {
                LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelSync.class, "Received Sync data...");
                barrel.getBarrelPopulate().insertSyncData((SyncData) data);
                LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelSync.class, "Finished synchronization");
                barrel.getLatch().countDown();
            }
        }
    }

    /**
     * Get the SyncRequest object with the last IDs
     * 
     * @return SyncRequest object with the last IDs
     */
    private SyncRequest getSyncRequest() {
        LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelSync.class, "Sending Sync request...");
        return new SyncRequest(barrel.getBarrelRetriever().getLastIDs());
    }

    /**
     * Get the data to be synchronized
     * 
     * @param syncRequest SyncRequest object with the last IDs
     * @return SyncData object with the data to be synchronized
     */
    private SyncData getSyncData(SyncRequest syncRequest) {
        LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelSync.class, "Sending Sync data...");
        List<Map<String, Object>> rows;
        SyncData syncData = new SyncData(new HashMap<>());
        for (String table : syncRequest.lastIDs().keySet()) {
            int lastID = syncRequest.lastIDs().get(table);
            rows = barrel.getBarrelRetriever().getTableWithStartID(table, lastID);

            syncData.tableResults().put(table, rows);
        }

        rows = barrel.getBarrelRetriever().getWeakTableWithStartID("website_urls",
                syncRequest.lastIDs().get("websites"));
        syncData.tableResults().put("website_urls", rows);

        rows = barrel.getBarrelRetriever().getWeakTableWithStartID("website_keywords",
                syncRequest.lastIDs().get("websites"));
        syncData.tableResults().put("website_keywords", rows);

        return syncData;
    }

    /**
     * Stop the BarrelSync
     */
    private void stop() {
        reliableMulticast.stopReceiving();
        reliableMulticast.stopSending();
    }

    // Getters and Setters

    /**
     * Set the running state of the BarrelSync
     * @param running the running state to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
}
