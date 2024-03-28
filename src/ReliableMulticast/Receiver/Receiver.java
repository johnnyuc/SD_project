package ReliableMulticast.Receiver;
import ReliableMulticast.Sender.*;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Receiver {

    // Socket
    private final MulticastSocket socket;

    // Queue for whatever the worker needs
    private final BlockingQueue<Object> workerQueue;

    // Sender
    private final Sender sender;

    // Constructor
    public Receiver(Sender sender, String multicastGroup, int port) throws IOException, InterruptedException {
        this.socket = new MulticastSocket(port);
        this.workerQueue = new LinkedBlockingQueue<>();
        this.sender = sender;

        // Join the multicast group
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(multicastGroup), port);
        socket.joinGroup(socketAddress, networkInterface);
    }

    // Method to start receiving data
    public void receive() throws InterruptedException {
        // Thread for ReceiverListener
        ReceiverListener receiverListener = new ReceiverListener(socket);
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