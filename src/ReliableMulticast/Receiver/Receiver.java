package ReliableMulticast.Receiver;

import ReliableMulticast.Sender.*;

// General imports
import java.net.*;
import java.nio.channels.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Error imports
import java.io.IOException;

// Network debugging imports
import java.util.Enumeration;

public class Receiver {
    // Multicast main objects
    // Sender
    private final Sender sender;
    // ReceiverListener
    private ReceiverListener receiverListener;
    // ReceiverWorker
    private ReceiverWorker receiverWorker;

    // Channel [DatagramChannel/Socket]
    private final DatagramChannel channel;

    // Queue for clean final data
    private final BlockingQueue<Object> workerQueue;

    // Constructor
    public Receiver(Sender sender, String multicastGroup, int port) throws IOException, InterruptedException {
        this.workerQueue = new LinkedBlockingQueue<>();
        this.sender = sender;

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            System.out.println(networkInterface);
        }

        // Join the multicast group using channel [non blocking]
        // NetworkInterface networkInterface =
        // NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName("172.23.173.134"));
        System.out.println("Network Interface: " + networkInterface);
        System.out.println("IP Address: " + Inet4Address.getLocalHost());
        InetAddress groupAddress = InetAddress.getByName(multicastGroup);
        this.channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
        this.channel.configureBlocking(false);
        this.channel.join(groupAddress, networkInterface);

        /*
         * this.channel = DatagramChannel.open();
         * this.channel.socket().setReuseAddress(true);
         * this.channel.socket().bind(new InetSocketAddress(port));
         * this.channel.configureBlocking(false);
         */ }

    
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

    // Getter for the workerQueue to the ReliableMulticast class
    // Used to store final received objects
    public BlockingQueue<Object> getWorkerQueue() {
        return workerQueue;
    }

    // Method to stop receiving data
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