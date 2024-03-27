package ReliableMulticast.Objects;

// Imports
import java.io.Serializable;

// Packet class
public class Container implements Serializable {
    // Fields
    private byte[] data;
    private Class<?> dataType;
    private String dataID;
    private String senderIP;
    private int packetNumber;
    private int totalPackets;
    private boolean retransmit;

    // Default Constructor
    public Container(byte[] data, Class<?> dataType, String dataID, String senderIP, int packetNumber, int totalPackets) {
        this.data = data;
        this.dataType = dataType;
        this.dataID = dataID;
        this.senderIP = senderIP;
        this.packetNumber = packetNumber;
        this.totalPackets = totalPackets;
    }

    // Retransmit constructor
    public Container(String dataID, String senderIP, int packetNumber) {
        this.dataID = dataID;
        this.senderIP = senderIP;
        this.packetNumber = packetNumber;
        this.retransmit = true;
    }

    // Getters and setters
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Class<?> getDataType() {
        return dataType;
    }

    public void setDataType(Class<?> dataType) {
        this.dataType = dataType;
    }

    public String getDataID() {
        return dataID;
    }

    public void setDataID(String dataID) {
        this.dataID = dataID;
    }

    public String getSenderIP() {
        return senderIP;
    }

    public void setSenderIP(String downloaderIP) {
        this.senderIP = senderIP;
    }

    public int getPacketNumber() {
        return packetNumber;
    }

    public void setPacketNumber(int packetNumber) {
        this.packetNumber = packetNumber;
    }

    public int getTotalPackets() {
        return totalPackets;
    }

    public void setTotalPackets(int totalPackets) {
        this.totalPackets = totalPackets;
    }

    public boolean isRetransmit() {
        return retransmit;
    }

    public void setRetransmit(boolean retransmit) {
        this.retransmit = retransmit;
    }

    // Boolean method to check if the packet is the last packet
    public boolean isLastPacket() {
        return packetNumber == totalPackets - 1;
    }
}