package ReliableMulticast.Receiver;

// General imports
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Poison pilling
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Logging
import ReliableMulticast.LogUtil;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 512;

    // Lock for atomic operations
    private final Lock lock = new ReentrantLock();

    // Channel
    private final DatagramChannel channel;

    // Queue for worker to get data from
    private final BlockingQueue<Object> listenerQueue = new LinkedBlockingQueue<>();

    // Running flag
    private boolean running = true;

    public ReceiverListener(DatagramChannel channel) {
        this.channel = channel;
    }

    @Override
    public void run() {
        try {
            while (running) {
                receivePacket().ifPresent(listenerQueue::add);
            }
        } catch (IOException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
        }
    }

    private Optional<byte[]> receivePacket() throws IOException {
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

    public byte[] getDataFromQueue() {
        try {
            return (byte[]) listenerQueue.take();
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
            return null;
        }
    }

    public void stop() {
        lock.lock();
        try {
            this.running = false;
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
            System.out.println("ReceiverListener stopped");
        }
    }
}