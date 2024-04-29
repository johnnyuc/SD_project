package Server.Controller;

// Package imports
import Server.IndexStorageBarrel.IndexStorageBarrelInterface;

// Logging imports
import Logger.LogUtil;

/**
 * Represents a BarrelTimestamp object that holds information about a remote
 * barrel and its timestamp.
 */
public class BarrelTimestamp {
    /**
     * The remote barrel object associated with this BarrelTimestamp.
     */
    private final IndexStorageBarrelInterface remoteBarrel;
    /**
     * The timestamp value of this BarrelTimestamp.
     */
    private long timestamp;
    /**
     * The average response time of this BarrelTimestamp.
     */
    private long avgResponseTime = 0;
    /**
     * The barrel ID of this BarrelTimestamp.
     */
    private final int barrelID;

    /**
     * Constructs a BarrelTimestamp object with the specified remote barrel,
     * timestamp, and barrel ID.
     * 
     * @param remoteBarrel the remote barrel object
     * @param timestamp    the timestamp value
     * @param barrelId     the barrel ID
     */
    public BarrelTimestamp(IndexStorageBarrelInterface remoteBarrel, long timestamp, int barrelId) {
        this.remoteBarrel = remoteBarrel;
        this.timestamp = timestamp;
        this.barrelID = barrelId;
    }

    /**
     * Returns the remote barrel object associated with this BarrelTimestamp.
     * 
     * @return the remote barrel object
     */
    public IndexStorageBarrelInterface getRemoteBarrel() {
        return remoteBarrel;
    }

    /**
     * Returns the timestamp value of this BarrelTimestamp.
     * 
     * @return the timestamp value
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp value of this BarrelTimestamp.
     * 
     * @param timestamp the new timestamp value
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the barrel ID of this BarrelTimestamp.
     * 
     * @return the barrel ID
     */
    public int getBarrelID() {
        return barrelID;
    }

    /**
     * Returns the average response time of this BarrelTimestamp.
     * 
     * @return the average response time
     */
    public long getAvgResponseTime() {
        LogUtil.logInfo(LogUtil.ANSI_GREEN, BarrelTimestamp.class, "returning response time:" + avgResponseTime);
        return avgResponseTime;
    }

    /**
     * Sets the average response time of this BarrelTimestamp.
     * 
     * @param avgResponseTime the new average response time
     */
    public void setAvgResponseTime(long avgResponseTime) {
        this.avgResponseTime = (this.avgResponseTime + avgResponseTime) / 2;
    }
}
