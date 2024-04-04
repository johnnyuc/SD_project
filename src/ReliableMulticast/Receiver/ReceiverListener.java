package ReliableMulticast.Receiver;

// General imports
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Error imports
import java.io.IOException;
import java.util.Arrays;
// Stop imports
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Logger.LogUtil;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;

    // Channel
    private final MulticastSocket socket;

    // Queue for worker to get data from
    private final BlockingQueue<Object> listenerQueue = new LinkedBlockingQueue<>();

    // Running flag
    private volatile boolean running = true;

    // Constructor
    public ReceiverListener(MulticastSocket socket) {
        this.socket = socket;
    }

    // Thread startup
    @Override
    public void run() {
        try {
            while (running) {
                receiveContainer().ifPresent(listenerQueue::add);
            }
        } catch (IOException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, ReceiverListener.class, e);
        }

        LogUtil.logInfo(LogUtil.ANSI_CYAN, ReceiverListener.class, "ReceiverListener thread stopped");
    }

    // Method to receive a container
    private Optional<byte[]> receiveContainer() throws IOException {
        if (!running) {
            return Optional.empty();
        }

        byte[] buffer = new byte[MAX_PACKET_SIZE * 2];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        // Check if data was received
        if (packet.getLength() > 0) {
            // Trim the buffer to the actual size of the received data
            byte[] receivedData = Arrays.copyOf(buffer, packet.getLength());

            return Optional.of(receivedData);
        } else {
            // No data was available to read, return Optional.empty
            return Optional.empty();
        }
    }

    // Method to retreive data to the worker
    public Object getData() {
        try {
            return listenerQueue.take();
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, ReceiverListener.class, e);
            return null;
        }
    }

    // Method to put data into the listenerQueue
    // The only purpose it serves is for the worker to put a STOP_PILL
    public void putData(Object obj) {
        listenerQueue.add(obj);
    }

    // Method to stop the listener
    public void stop() {
        // Stop the listener
        this.running = false;
    }
}