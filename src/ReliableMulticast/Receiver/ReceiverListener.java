package ReliableMulticast.Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.Semaphore;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 256;

    // Socket
    private final MulticastSocket socket;

    // Queue for worker to get data from
    private final SynchronousQueue<byte[]> listenerQueue;

    // Semaphore to control access to the queue
    private final Semaphore semaphore;

    // Flag to control the listener's execution
    private volatile boolean running = true;

    public ReceiverListener(MulticastSocket socket, SynchronousQueue<byte[]> listenerQueue) {
        this.socket = socket;
        this.listenerQueue = listenerQueue;
        this.semaphore = new Semaphore(1); // Only one thread can access the queue at a time

        // Start the listener thread
        new Thread(this).start();

        // In case of CTRL+C, set running to false
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
    }

    @Override
    public void run() {
        try {
            while (running) {
                // STATUS OF THE LOCK
                System.out.println("Unlocked? " + semaphore.tryAcquire());
                semaphore.acquire(); // Acquire a permit before accessing the queue
                listenerQueue.add(receivePacket());
                semaphore.release(); // Release the permit after accessing the queue
            }
        } catch (IOException | InterruptedException e) {
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
        System.out.println("Received packet with size: " + packet.getLength() + " bytes");

        return packet.getData();
    }

    public byte[] getDataFromQueue() {
        try {
            semaphore.acquire(); // Acquire a permit before accessing the queue
            byte[] data = listenerQueue.take();
            semaphore.release(); // Release the permit after accessing the queue
            return data;
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }
}