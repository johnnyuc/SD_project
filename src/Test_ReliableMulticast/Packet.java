package Test_ReliableMulticast;

import java.io.Serializable;

public class Packet implements Serializable {
    private byte[] data;
    private boolean isLastPacket;

    public Packet(byte[] data, boolean isLastPacket) {
        this.data = data;
        this.isLastPacket = isLastPacket;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isLastPacket() {
        return isLastPacket;
    }
}