package ReliableMulticast.Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ReliableMulticast.LogUtil;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 512;

    // Socket
    private final MulticastSocket socket;

    // Queue for worker to get data from
    private final BlockingQueue<Object> listenerQueue = new LinkedBlockingQueue<>();

    public ReceiverListener(MulticastSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            while (true) {
                listenerQueue.add(receivePacket());
            }
        } catch (IOException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
        } finally {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
    }

    private byte[] receivePacket() throws IOException {
        byte[] buffer = new byte[MAX_PACKET_SIZE + MAX_PACKET_OVERHEAD];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        return packet.getData();
    }

    public byte[] getDataFromQueue() {
        try {
            return (byte[]) listenerQueue.take();
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
            return null;
        }
    }

}