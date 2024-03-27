package ReliableMulticast.Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.SynchronousQueue;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 256;

    // Socket
    private final MulticastSocket socket;

    // Queue for worker to get data from
    private final SynchronousQueue<byte[]> dataQueue;

    // Flag to control the listener's execution
    private volatile boolean running = true;

    public ReceiverListener(MulticastSocket socket, SynchronousQueue<byte[]> dataQueue) {
        this.socket = socket;
        this.dataQueue = dataQueue;

        // Start the listener thread
        new Thread(this).start();

        // In case of CTRL+C, set running to false
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
    }

    @Override
    public void run() {
        try {
            while (running)
                dataQueue.add(receivePacket());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.err.println("Error: " + e.getMessage());
        } finally {
            running = false;
            socket.close();
        }
    }

    private byte[] receivePacket() throws IOException {
        byte[] buffer = new byte[MAX_PACKET_SIZE + MAX_PACKET_OVERHEAD];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        // TODO: Pensar se damos trim ao buffer
        // byte[] data = Arrays.copyOf(buffer, packet.getLength());
        return packet.getData();
    }

    public byte[] getDataFromQueue() {
        try {
            // Retrieve data from the queue when available
            return dataQueue.take();
        } catch (InterruptedException e) {
            // TODO: Handle interruption
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }
}
