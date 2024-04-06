package ReliableMulticast.Objects;

/**
 * Represents a container timestamp, which contains an array of containers and a
 * timestamp value.
 */
public class ContainersTimestamp {
    /**
     * The array of containers.
     */
    private final Container[] containers;
    /**
     * The timestamp value.
     */
    private long timestamp;

    /**
     * Constructs a new ContainersTimestamp object with the specified containers and
     * timestamp.
     *
     * @param containers an array of containers
     * @param timestamp  the timestamp value
     */
    public ContainersTimestamp(Container[] containers, long timestamp) {
        this.containers = containers;
        this.timestamp = timestamp;
    }

    // Getters and setters

    /**
     * Returns the array of containers.
     *
     * @return the array of containers
     */
    public Container[] getContainers() {
        return containers;
    }

    /**
     * Returns the timestamp value.
     *
     * @return the timestamp value
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp value.
     *
     * @param timestamp the timestamp value
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}