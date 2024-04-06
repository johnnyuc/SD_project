package ReliableMulticast;

// Package imports
import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Receiver.Receiver;
import ReliableMulticast.Receiver.ReceiverWorker;

// Logging imports
import Logger.LogUtil;

// General imports
import java.util.UUID;

// Error imports
import java.io.IOException;

/**
 * The ReliableMulticast class provides a reliable multicast communication
 * mechanism.
 * It allows sending and receiving data over a multicast group using a sender
 * and receiver.
 */
public class ReliableMulticast {

    // Variables
    /**
     * The sender used to send data over the multicast group.
     */
    private Sender sender;
    /**
     * The receiver used to receive data from the multicast group.
     */
    private Receiver receiver;

    /**
     * Constructs a ReliableMulticast object with the specified parameters.
     *
     * @param interfaceAddress the interface address to bind the receiver to
     * @param multicastGroup   the multicast group to join
     * @param port             the port number to use for communication
     * @param senderClass      the class of the sender
     * @param ignoredClasses   an array of classes to ignore when receiving data
     */
    public ReliableMulticast(String interfaceAddress, String multicastGroup, int port,
            Class<?> senderClass, Class<?>[] ignoredClasses) {
        UUID multicastID = UUID.randomUUID();
        try {
            // Creates sender and receiver
            this.sender = new Sender(multicastGroup, port, senderClass, multicastID);
            this.receiver = new Receiver(sender, interfaceAddress, multicastGroup, port, ignoredClasses, multicastID);
        } catch (IOException | InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_RED, ReliableMulticast.class, e);
        }
    }

    /**
     * Sends the specified object over the multicast group.
     *
     * @param object the object to send
     */
    public void send(Object object) {
        sender.getSendBuffer().add(object);
    }

    /**
     * Stops sending data.
     */
    public void stopSending() {
        sender.stop();
    }

    /**
     * Starts receiving data.
     */
    public void startReceiving() {
        try {
            receiver.receive();
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, ReliableMulticast.class, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stops receiving data.
     */
    public void stopReceiving() {
        receiver.stop();
    }

    /**
     * Retrieves the next available data object.
     *
     * @return the next available data object, or null if protocol is shutting down
     */
    public Object getData() {
        try {
            Object data = receiver.getWorkerQueue().take();
            if (data == ReceiverWorker.STOP_PILL) {
                stopReceiving();
                return null;
            }
            return data;
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, ReliableMulticast.class, e);
            return null;
        }
    }
}