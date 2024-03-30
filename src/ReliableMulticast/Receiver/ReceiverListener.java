package ReliableMulticast.Receiver;

// General imports
import java.nio.ByteBuffer;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Error imports
import java.io.IOException;

// Stop imports
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Logger.LogUtil;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 512;

    // Channel
    private final DatagramChannel channel;

    // Queue for worker to get data from
    private final BlockingQueue<Object> listenerQueue = new LinkedBlockingQueue<>();

    // Running flag
    private volatile boolean running = true;

    // Stopping the thread
    private final Lock lock = new ReentrantLock();

    // Constructor
    public ReceiverListener(DatagramChannel channel) {
        this.channel = channel;
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
        lock.lock();
        try {
            if (!running) {
                return Optional.empty();
            }

            ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE + MAX_PACKET_OVERHEAD);

            // Non-blocking receive
            SocketAddress senderAddress = channel.receive(buffer);

            if (senderAddress != null) {
                // Flip the buffer to prepare it for get operations
                buffer.flip();

                // Convert ByteBuffer to byte array
                byte[] byteArray = new byte[buffer.remaining()];
                buffer.get(byteArray);

                return Optional.of(byteArray);
            } else {
                // No data was available to read, return Optional.empty
                return Optional.empty();
            }
        } finally {
            lock.unlock();
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
        lock.lock();
        try {
            // Stop the listener
            this.running = false;
        } finally {
            lock.unlock();
        }
    }
}