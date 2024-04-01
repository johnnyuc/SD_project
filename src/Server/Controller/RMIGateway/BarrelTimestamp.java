package Server.Controller.RMIGateway;

import Server.IndexStorageBarrel.IndexStorageBarrelInterface;

public class BarrelTimestamp {
    private IndexStorageBarrelInterface remoteBarrel;

    private long timestamp;
    private int barrelID;

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
}
