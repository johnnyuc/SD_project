package ReliableMulticast.Objects;

// Imports
import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a container object that holds data for reliable multicast
 * communication.
 */
public class Container implements Serializable {
    // Fields
    private final byte[] data;
    private final Class<?> dataType;
    private final Class<?> senderClass;
    private final String dataID;
    private final UUID multicastID;
    private final int packetNumber;
    private final int totalPackets;

    /**
     * Constructs a new Container object with the specified data and metadata.
     *
     * @param data         the byte array representing the data
     * @param dataType     the class type of the data
     * @param senderClass  the class type of the sender
     * @param dataID       the ID of the data
     * @param multicastID  the ID of the multicast communication
     * @param packetNumber the number of the packet
     * @param totalPackets the total number of packets
     */
    public Container(byte[] data, Class<?> dataType, Class<?> senderClass,
            String dataID, UUID multicastID, int packetNumber, int totalPackets) {
        this.data = data;
        this.dataType = dataType;
        this.senderClass = senderClass;
        this.dataID = dataID;
        this.multicastID = multicastID;
        this.packetNumber = packetNumber;
        this.totalPackets = totalPackets;
    }

    // Getters and setters

    /**
     * Returns the byte array representing the data.
     *
     * @return the data as a byte array
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the class type of the data.
     *
     * @return the class type of the data
     */
    public Class<?> getDataType() {
        return dataType;
    }

    /**
     * Returns the class type of the sender.
     *
     * @return the class type of the sender
     */
    public Class<?> getSenderClass() {
        return senderClass;
    }

    /**
     * Returns the ID of the data.
     *
     * @return the ID of the data
     */
    public String getDataID() {
        return dataID;
    }

    /**
     * Returns the ID of the multicast communication.
     *
     * @return the ID of the multicast communication
     */
    public UUID getMulticastID() {
        return multicastID;
    }

    /**
     * Returns the number of the packet.
     *
     * @return the number of the packet
     */
    public int getPacketNumber() {
        return packetNumber;
    }

    /**
     * Returns the total number of packets.
     *
     * @return the total number of packets
     */
    public int getTotalPackets() {
        return totalPackets;
    }

    /**
     * Checks if the packet is the last packet.
     *
     * @return true if the packet is the last packet, false otherwise
     */
    public boolean isLastPacket() {
        return packetNumber == totalPackets - 1;
    }
}