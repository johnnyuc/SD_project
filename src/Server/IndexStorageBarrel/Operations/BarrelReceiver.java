package Server.IndexStorageBarrel.Operations;

import java.sql.SQLException;

import Logger.LogUtil;
import ReliableMulticast.ReliableMulticast;
import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.IndexStorageBarrel;

/**
 * The BarrelReceiver class is responsible for receiving data from a
 * ReliableMulticast object
 * and inserting it into an IndexStorageBarrel object.
 */
public class BarrelReceiver implements Runnable {
    private final IndexStorageBarrel barrel;
    private final ReliableMulticast reliableMulticast;
    private boolean running = true;

    /**
     * Constructor for BarrelReceiver
     * 
     * @param barrel            the IndexStorageBarrel object to insert data into
     * @param reliableMulticast the ReliableMulticast object to receive data
     */
    public BarrelReceiver(IndexStorageBarrel barrel, ReliableMulticast reliableMulticast) {
        this.barrel = barrel;
        this.reliableMulticast = reliableMulticast;
        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        new Thread(this, "Barrel Receiver").start();
    }

    /**
     * Starts the receiving process and inserts the received data into the
     * IndexStorageBarrel.
     */
    @Override
    public void run() {
        reliableMulticast.startReceiving();
        while (running) {
            try {

                CrawlData crawlData = (CrawlData) reliableMulticast.getData();
                if (crawlData == null) {
                    running = false;
                    continue;
                }

                LogUtil.logInfo(LogUtil.ANSI_WHITE, BarrelReceiver.class, "Received data: " + crawlData.getUrl());
                barrel.getBarrelPopulate().insertCrawlData(crawlData);

            } catch (SQLException e) {
                LogUtil.logError(LogUtil.ANSI_RED, BarrelReceiver.class, e);
            }
        }
        stop();
    }

    /**
     * Stops the receiving and sending processes of the ReliableMulticast object.
     */
    private void stop() {
        reliableMulticast.stopReceiving();
        reliableMulticast.stopSending();
    }

    /**
     * Sets the running state of the BarrelReceiver.
     * 
     * @param running the running state to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
}
