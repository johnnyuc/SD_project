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
        this.channel.join(groupAddress, networkInterface);
    }

    // Method to start receiving data
    public void receive() throws InterruptedException, IOException {
        // Thread for ReceiverListener
        ReceiverListener receiverListener = new ReceiverListener(channel);
        Thread listenerThread = new Thread(receiverListener, "Multicast Listener Thread");
        listenerThread.start();

        // Thread for ReceiverWorker
        ReceiverWorker receiverWorker = new ReceiverWorker(sender, receiverListener, workerQueue);
        Thread workerThread = new Thread(receiverWorker, "Multicast Worker Thread");
        workerThread.start();
    }

    public BlockingQueue<Object> getWorkerQueue() {
        return workerQueue;
    }
}