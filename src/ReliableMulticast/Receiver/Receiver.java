package ReliableMulticast.Receiver;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;

public class Receiver {

    // Socket
    private final MulticastSocket socket;

    // Queue for worker to get data from
    private SynchronousQueue<byte[]> dataQueue;

    // Constructor
    public Receiver(String multicastGroup, int port) throws IOException {
        this.socket = new MulticastSocket(port);

        // Join the multicast group
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(multicastGroup), port);
        socket.joinGroup(socketAddress, networkInterface);

        // Thread for ReceiverListener
        ReceiverListener receiverListener = new ReceiverListener(socket, dataQueue);
        new Thread(receiverListener).start();

        // Thread for ReceiverWorker
        ReceiverWorker receiverWorker = new ReceiverWorker(receiverListener, dataQueue);
        new Thread(receiverWorker).start();
    }

    // Method to close the socket
    public void close() {
        socket.close();
    }
}