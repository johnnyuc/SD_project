package ReliableMulticast.Receiver;

// Package imports
import ReliableMulticast.Sender.*;

// General imports
import java.net.*;
import java.util.UUID;
import java.nio.channels.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Exception imports
import java.io.IOException;

/**
 * The Receiver class represents a receiver in a reliable multicast system.
 * It receives data from a multicast group and processes it using a worker
 * thread.
 */
public class Receiver {
    // Multicast main objects
    /**
     * The Sender object used to send retransmission requests.
     */
    private final Sender sender;
    /**
     * The ReceiverListener used to listen for incoming data.
     */
    private ReceiverListener receiverListener;
    /**
     * The ReceiverWorker used to process received data.
     */
    private ReceiverWorker receiverWorker;

    /**
     * The DatagramChannel used to receive data.
     */
    private final DatagramChannel channel;

    /**
     * The unique identifier for the multicast session.
     */
    private final UUID multicastID;

    /**
     * The workerQueue used to store final received objects.
     */
    private final BlockingQueue<Object> workerQueue;

    /**
     * An array of classes to ignore when processing received data.
     */
    private final Class<?>[] ignoredClasses;

    /**
     * Constructs a Receiver object.
     *
     * @param sender           the sender object used to send retransmission
     *                         requests
     * @param interfaceAddress the IP address of the network interface to use for
     *                         multicast
     * @param multicastGroup   the IP address of the multicast group
     * @param port             the port number to bind the receiver to
     * @param ignoredClasses   an array of classes to ignore when processing
     *                         received data
     * @param multicastID      the unique identifier for the multicast session
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public Receiver(Sender sender, String interfaceAddress, String multicastGroup,
            int port, Class<?>[] ignoredClasses, UUID multicastID)
            throws IOException, InterruptedException {
        this.workerQueue = new LinkedBlockingQueue<>();
        this.sender = sender;
        this.ignoredClasses = ignoredClasses;
        this.multicastID = multicastID;

        // Join the multicast group using channel [non blocking]
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(interfaceAddress));
        InetAddress groupAddress = InetAddress.getByName(multicastGroup);
        this.channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
        this.channel.configureBlocking(false);
        this.channel.join(groupAddress, networkInterface);
    }

    /**
     * Starts receiving data from the multicast group.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws IOException          if an I/O error occurs
     */
    public void receive() throws InterruptedException, IOException {
        // Thread for ReceiverListener
        receiverListener = new ReceiverListener(channel);
        Thread listenerThread = new Thread(receiverListener, "Multicast Listener Thread");
        listenerThread.start();

        // Thread for ReceiverWorker
        receiverWorker = new ReceiverWorker(sender, receiverListener, workerQueue, ignoredClasses, multicastID);
        Thread workerThread = new Thread(receiverWorker, "Multicast Worker Thread");
        workerThread.start();
    }

    /**
     * Gets the workerQueue used to store final received objects.
     *
     * @return the workerQueue
     */
    public BlockingQueue<Object> getWorkerQueue() {
        return workerQueue;
    }

    /**
     * Stops receiving data and cleans up resources.
     */
    public void stop() {
        try {
            // Kill sub-threads
            if (receiverListener != null)
                receiverListener.stop();
            if (receiverWorker != null)
                receiverWorker.stop();

            // Close the channel/socket
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}