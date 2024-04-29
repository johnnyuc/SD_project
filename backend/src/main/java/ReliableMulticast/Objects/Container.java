package ReliableMulticast.Objects;

// General imports
import java.util.UUID;
import java.io.Serializable;

/**
 * The Container class represents a container for data to be sent over a network.
 *
 * @param data         the byte array representing the data
 * @param dataType     the class type of the data
 * @param senderClass  the class type of the sender
 * @param dataID       the ID of the data
 * @param multicastID  the ID of the multicast communication
 * @param packetNumber the number of the packet
 * @param totalPackets the total number of packets
 */

public record Container(byte[] data, Class<?> dataType, Class<?> senderClass, String dataID, UUID multicastID,
                        int packetNumber, int totalPackets) implements Serializable {

    // Getters and setters

    /**
     * Returns the byte array representing the data.
     *
     * @return the data as a byte array
     */
    @Override
    public byte[] data() {
        return data;
    }

    /**
     * Returns the class type of the data.
     *
     * @return the class type of the data
     */
    @Override
    public Class<?> dataType() {
        return dataType;
    }

    /**
     * Returns the class type of the sender.
     *
     * @return the class type of the sender
     */
    @Override
    public Class<?> senderClass() {
        return senderClass;
    }

    /**
     * Returns the ID of the data.
     *
     * @return the ID of the data
     */
    @Override
    public String dataID() {
        return dataID;
    }

    /**
     * Returns the ID of the multicast communication.
     *
     * @return the ID of the multicast communication
     */
    @Override
    public UUID multicastID() {
        return multicastID;
    }

    /**
     * Returns the number of the packet.
     *
     * @return the number of the packet
     */
    @Override
    public int packetNumber() {
        return packetNumber;
    }

    /**
     * Returns the total number of packets.
     *
     * @return the total number of packets
     */
    @Override
    public int totalPackets() {
        return totalPackets;
    }

}