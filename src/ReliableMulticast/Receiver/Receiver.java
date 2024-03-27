package ReliableMulticast.Receiver;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;

public class Receiver {

    // Socket
    private final MulticastSocket socket;

    // Constructor
    public Receiver(String multicastGroup, int port) throws IOException, InterruptedException {
        this.socket = new MulticastSocket(port);

        // Initialize the queues
        // Queue for container objects
        SynchronousQueue<byte[]> listenerQueue = new SynchronousQueue<>();
        // Queue for whatever the worker needs
        SynchronousQueue<byte[]> workerQueue = new SynchronousQueue<>();

        // Join the multicast group
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(multicastGroup), port);
        socket.joinGroup(socketAddress, networkInterface);

        // Thread for ReceiverListener
        ReceiverListener receiverListener = new ReceiverListener(socket, listenerQueue);
        new Thread(receiverListener).start();

        // Thread for ReceiverWorker
        ReceiverWorker receiverWorker = new ReceiverWorker(receiverListener, workerQueue);
        new Thread(receiverWorker).start();

        // Continuous print of the received data
        while (true) {
            byte[] receivedData = workerQueue.take();
            System.out.println("Received data: " + new String(receivedData));
        }
    }

    // Method to close the socket
    public void close() {
        socket.close();
    }
}