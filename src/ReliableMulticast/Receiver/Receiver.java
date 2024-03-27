package ReliableMulticast.Receiver;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ReliableMulticast.Objects.CrawlData;

public class Receiver {

    // Socket
    private final MulticastSocket socket;

    // Queue for whatever the worker needs
    private final BlockingQueue<Object> workerQueue;

    // Flag to control the receiver's execution
    private volatile boolean running = true;

    // CountDownLatch to ensure receive method has finished before closing the socket
    private final CountDownLatch latch = new CountDownLatch(1);

    // Constructor
    public Receiver(String multicastGroup, int port) throws IOException, InterruptedException {
        this.socket = new MulticastSocket(port);
        this.workerQueue = new LinkedBlockingQueue<>();

        // Join the multicast group
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(multicastGroup), port);
        socket.joinGroup(socketAddress, networkInterface);
    }

    // Method to start receiving data
    public void receive() throws InterruptedException {
        // Thread for ReceiverListener
        ReceiverListener receiverListener = new ReceiverListener(socket);
        new Thread(receiverListener).start();

        // Thread for ReceiverWorker
        ReceiverWorker receiverWorker = new ReceiverWorker(receiverListener, workerQueue);
        new Thread(receiverWorker).start();

        // Continuous print of the received data
        while (running && !socket.isClosed()) {
            System.out.println(running);
            Object obj = workerQueue.poll(1, TimeUnit.SECONDS);
            if (obj != null) {
                if (obj instanceof CrawlData receivedData) {
                    System.out.println("Received data: " + receivedData.getUrl());
                } else {
                    System.out.println("Unexpected object in queue: " + obj);
                }
            }
        }

        // Count down the latch to signal that the receive method has finished
        latch.countDown();
    }

    // Method to close the socket
    public void close() {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    // Add a method to stop receiving
    public void stopReceiving() {
        running = false;

        // Wait for the receive method to finish
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        }

        // Close the socket
        close();
    }
}