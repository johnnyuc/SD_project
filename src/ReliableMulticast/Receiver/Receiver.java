package ReliableMulticast.Receiver;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ReliableMulticast.Objects.*;
import ReliableMulticast.Sender.*;

public class Receiver {

    // Socket
    private final MulticastSocket socket;

    // Queue for whatever the worker needs
    private final BlockingQueue<Object> workerQueue;

    // Sender
    private final Sender sender;

    private ReceiverListener receiverListener;
    private Thread listenerThread;

    private ReceiverWorker receiverWorker;
    private Thread workerThread;

    // CountDownLatch to ensure receive method has finished before closing the
    // socket
    private final CountDownLatch latch = new CountDownLatch(1);

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
        receiverListener = new ReceiverListener(socket);
        listenerThread = new Thread(receiverListener);
        listenerThread.start();

        // Thread for ReceiverWorker
        receiverWorker = new ReceiverWorker(sender, receiverListener, workerQueue);
        workerThread = new Thread(receiverWorker);
        workerThread.start();
    }

    // Method to close the socket
    public void close() {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    // Add a method to stop receiving
    public void stopReceiving() {
        // Stop the listener and worker threads
        receiverListener.stop();
        receiverWorker.stop();

        // Wait for the threads to finish
        // TODO: GETTING STUCK IN HERE
        // THREADS SHOULD CLOSE PROPERLY USING JOIN (NOT WORKING YET, NEED TO FIX THIS)
        try {
            listenerThread.join();
            workerThread.join();
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        }

        // Count down the latch to signal that the receive method has finished
        latch.countDown();

        // Wait for the receive method to finish
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        }

        // Close the socket
        close();
    }

    public BlockingQueue<Object> getWorkerQueue() {
        return workerQueue;
    }
}