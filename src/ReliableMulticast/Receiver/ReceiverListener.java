package ReliableMulticast.Receiver;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ReliableMulticast.LogUtil;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 512;

    // Channel
    private final DatagramChannel channel;

    // Queue for worker to get data from
    private final BlockingQueue<Object> listenerQueue = new LinkedBlockingQueue<>();

    public ReceiverListener(DatagramChannel channel) {
        this.channel = channel;
    }

    @Override
    public void run() {
        try {
            while (true) {
                listenerQueue.add(Objects.requireNonNull(receivePacket()));
            }
        } catch (IOException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
        }
    }

    private byte[] receivePacket() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE + MAX_PACKET_OVERHEAD);

        // Non-blocking receive
        SocketAddress senderAddress = channel.receive(buffer);

        if (senderAddress != null) {
            // Flip the buffer to prepare it for get operations
            buffer.flip();

            // Convert ByteBuffer to byte array
            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            return byteArray;
        } else {
            // No data was available to read, return null
            return null;
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
}