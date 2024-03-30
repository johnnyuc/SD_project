package Server.Controller.RMIGateway;

import Server.IndexStorageBarrel.IndexStorageBarrel;

public class BarrelTimestamp {
    private IndexStorageBarrel barrel;
    private long timestamp;

    public BarrelTimestamp(IndexStorageBarrel barrel, long timestamp) {
        this.barrel = barrel;
        this.timestamp = timestamp;
    }

    public IndexStorageBarrel getBarrel() {
        return barrel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
