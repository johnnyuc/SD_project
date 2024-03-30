package ReliableMulticast.Objects;

public class ContainersTimestamp {
    private Container[] containers;
    private long timestamp;

    public ContainersTimestamp(Container[] containers, long timestamp) {
        this.containers = containers;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public Container[] getContainers() {
        return containers;
    }

    public void setContainers(Container[] containers) {
        this.containers = containers;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}