package ReliableMulticast.Objects;

// Imports
import java.io.Serializable;
import java.util.UUID;

// Packet class
public class Container implements Serializable {
    // Fields
    private final byte[] data;
    private final Class<?> dataType;
    private final Class<?> senderClass;
    private final String dataID;
    private final UUID multicastID;
    private final int packetNumber;
    private final int totalPackets;

    // Default Constructor
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
    public byte[] getData() {
        return data;
    }

    public Class<?> getDataType() {
        return dataType;
    }

    public Class<?> getSenderClass() {
        return senderClass;
    }

    public String getDataID() {
        return dataID;
    }

    public UUID getMulticastID() {
        return multicastID;
    }

    public int getPacketNumber() {
        return packetNumber;
    }

    public int getTotalPackets() {
        return totalPackets;
    }

    // Boolean method to check if the packet is the last packet
    public boolean isLastPacket() {
        return packetNumber == totalPackets - 1;
    }
}