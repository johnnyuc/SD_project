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

/**
 * The ReceiverListener class is responsible for receiving data from a
 * DatagramChannel and providing it to a worker thread.
 */
public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;

    // Channel
    private final DatagramChannel channel;

    // Queue for worker to get data from
    private final BlockingQueue<Object> listenerQueue = new LinkedBlockingQueue<>();

    // Running flag
    private volatile boolean running = true;

    // Stopping the thread
    private final Lock lock = new ReentrantLock();

    /**
     * Constructs a ReceiverListener object with the specified DatagramChannel.
     *
     * @param channel the DatagramChannel to receive data from
     */
    public ReceiverListener(DatagramChannel channel) {
        this.channel = channel;
    }

    /**
     * Starts the ReceiverListener thread and continuously receives data until
     * stopped.
     */
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

    /**
     * Receives a container from the DatagramChannel.
     *
     * @return an Optional containing the received byte array, or an empty Optional
     *         if no data was available
     * @throws IOException if an I/O error occurs while receiving the container
     */
    private Optional<byte[]> receiveContainer() throws IOException {
        lock.lock();
        try {
            if (!running) {
                return Optional.empty();
            }

            ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE * 2);

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

    /**
     * Retrieves data from the listenerQueue.
     *
     * @return the retrieved data
     */
    public Object getData() {
        try {
            return listenerQueue.take();
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, ReceiverListener.class, e);
            return null;
        }
    }

    /**
     * Puts data into the listenerQueue.
     *
     * @param obj the data to be put into the listenerQueue
     */
    public void putData(Object obj) {
        listenerQueue.add(obj);
    }

    /**
     * Stops the ReceiverListener thread.
     */
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