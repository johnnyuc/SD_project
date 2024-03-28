package ReliableMulticast.Receiver;
import ReliableMulticast.Sender.*;

import java.net.*;
import java.io.IOException;
import java.nio.channels.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Receiver {

    // Channel
    private final DatagramChannel channel;

    // Queue for whatever the worker needs
    private final BlockingQueue<Object> workerQueue;

    // Sender
    private final Sender sender;

    // ReceiverListener
    private ReceiverListener receiverListener;
    // ReceiverWorker
    private ReceiverWorker receiverWorker;

    // Constructor
    public Receiver(Sender sender, String multicastGroup, int port) throws IOException, InterruptedException {
        this.workerQueue = new LinkedBlockingQueue<>();
        this.sender = sender;

        // Join the multicast group
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetAddress groupAddress = InetAddress.getByName(multicastGroup);
        this.channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
        this.channel.configureBlocking(false);
        this.channel.join(groupAddress, networkInterface);
    }

    // Method to start receiving data
    public void receive() throws InterruptedException, IOException {
        // Thread for ReceiverListener
        receiverListener = new ReceiverListener(channel);
        Thread listenerThread = new Thread(receiverListener, "Multicast Listener Thread");
        listenerThread.start();

        // Thread for ReceiverWorker
        receiverWorker = new ReceiverWorker(sender, receiverListener, workerQueue);
        Thread workerThread = new Thread(receiverWorker, "Multicast Worker Thread");
        workerThread.start();
    }

    // Method to pass the workerQueue to the ReliableMulticast object
    public BlockingQueue<Object> getWorkerQueue() {
        return workerQueue;
    }

    public void stop() {
        try {
            // Kill sub-threads
            receiverListener.stop();
            receiverWorker.stop();

            // Close the channel/socket
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}