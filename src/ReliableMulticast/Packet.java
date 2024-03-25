package ReliableMulticast;

import java.io.Serializable;

public class Packet implements Serializable {
    private byte[] data;
    private String downloaderIP;
    private int downloaderID;
    private int packetNumber;
    private int totalPackets;

    public Packet(byte[] data, String downloaderIP, int downloaderID, int packetNumber, int totalPackets) {
        this.data = data;
        this.downloaderIP = downloaderIP;
        this.downloaderID = downloaderID;
        this.packetNumber = packetNumber;
        this.totalPackets = totalPackets;
    }

    // Getters and setters
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getDownloaderIP() {
        return downloaderIP;
    }

    public void setDownloaderIP(String downloaderIP) {
        this.downloaderIP = downloaderIP;
    }

    public int getDownloaderID() {
        return downloaderID;
    }

    public void setDownloaderID(int downloaderID) {
        this.downloaderID = downloaderID;
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

    // Boolean method to check if the packet is the last packet
    public boolean isLastPacket() {
        return packetNumber == totalPackets - 1;
    }

}