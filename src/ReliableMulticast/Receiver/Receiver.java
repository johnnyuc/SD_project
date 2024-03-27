package ReliableMulticast.Receiver;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ReliableMulticast.Objects.CrawlData;

public class Receiver {

    // Socket
    private final MulticastSocket socket;

    // Constructor
    public Receiver(String multicastGroup, int port) throws IOException, InterruptedException {
        this.socket = new MulticastSocket(port);

        // Queue for whatever the worker needs
        BlockingQueue<Object> workerQueue = new LinkedBlockingQueue<>();

        // Join the multicast group
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(multicastGroup), port);
        socket.joinGroup(socketAddress, networkInterface);

        // Thread for ReceiverListener
        ReceiverListener receiverListener = new ReceiverListener(socket);
        new Thread(receiverListener).start();

        // Thread for ReceiverWorker
        ReceiverWorker receiverWorker = new ReceiverWorker(receiverListener, workerQueue);
        new Thread(receiverWorker).start();

        // Continuous print of the received data
        while (true) {
            Object obj = workerQueue.take();
            if (obj instanceof CrawlData) {
                CrawlData receivedData = (CrawlData) obj;
                System.out.println("Received data: " + receivedData.getUrl());
            } else {
                System.out.println("Unexpected object in queue: " + obj);
            }
        }
    }

    // Method to close the socket
    public void close() {
        socket.close();
    }
}