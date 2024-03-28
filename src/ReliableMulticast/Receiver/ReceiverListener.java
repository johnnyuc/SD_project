package ReliableMulticast.Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 512;

    // Socket
    private final MulticastSocket socket;

    // Queue for worker to get data from
    private final BlockingQueue<byte[]> listenerQueue = new LinkedBlockingQueue<>();

    // Flag to control the listener's execution
    private volatile boolean running = true;

    public ReceiverListener(MulticastSocket socket) {
        this.socket = socket;

        // In case of CTRL+C, set running to false
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
    }

    @Override
    public void run() {
        try {
            while (running) {
                listenerQueue.add(receivePacket());
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
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

    public byte[] getDataFromQueue(){
        try {
            return listenerQueue.take();
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }

    public void stop() {
        running = false;
    }
}