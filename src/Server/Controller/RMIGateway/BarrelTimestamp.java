package Server.Controller.RMIGateway;

import Logger.LogUtil;
import Server.IndexStorageBarrel.IndexStorageBarrelInterface;

public class BarrelTimestamp {
    private final IndexStorageBarrelInterface remoteBarrel;
    private long timestamp;
    private long avgResponseTime = 0;

    private final int barrelID;

    public BarrelTimestamp(IndexStorageBarrelInterface remoteBarrel, long timestamp, int barrelId) {
        this.remoteBarrel = remoteBarrel;
        this.timestamp = timestamp;
        this.barrelID = barrelId;
    }

    public IndexStorageBarrelInterface getRemoteBarrel() {
        return remoteBarrel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getBarrelID() {
        return barrelID;
    }

    public long getAvgResponseTime() {
        LogUtil.logInfo(LogUtil.ANSI_GREEN, BarrelTimestamp.class, "returning response time:" + avgResponseTime);
        return avgResponseTime;
    }

    public void setAvgResponseTime(long avgResponseTime) {
        LogUtil.logInfo(LogUtil.ANSI_GREEN, BarrelTimestamp.class, "response time:" + avgResponseTime);
        LogUtil.logInfo(LogUtil.ANSI_GREEN, BarrelTimestamp.class,
                "setting response time:" + (this.avgResponseTime + avgResponseTime) / 2);
        this.avgResponseTime = (this.avgResponseTime + avgResponseTime) / 2;
        LogUtil.logInfo(LogUtil.ANSI_GREEN, BarrelTimestamp.class, "response time:" + this.avgResponseTime);
    }
}
