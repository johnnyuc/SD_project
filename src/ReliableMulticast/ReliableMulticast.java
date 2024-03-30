// TODO: REMOVE ALL THE TODO COMMENTS
// TODO: REMOVE ALL SYSTEM.OUT.PRINTLN LINES UNLESS STRICTLY NECESSARY FOR THE CODE
// TODO: FIX RELIABILITY ISSUES, INCLUDING LAST X CONTAINERS NOT BEING SEND

package ReliableMulticast;

// Multicast imports
import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Receiver.Receiver;
import ReliableMulticast.Receiver.ReceiverWorker;

// General imports
import java.net.InetAddress;

import Logger.LogUtil;

// Error imports
import java.io.IOException;

public class ReliableMulticast {

    // Variables
    private final Sender sender;
    private final Receiver receiver;

    // Constructor
    public ReliableMulticast(String multicastGroup, int port) {
        try {
            // Gets machine IP
            String senderIP = InetAddress.getLocalHost().getHostAddress();
            // Creates sender and receiver
            this.sender = new Sender(multicastGroup, port, senderIP);
            this.receiver = new Receiver(sender, multicastGroup, port);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to start sending data
    public void send(Object object) {
        sender.getSendBuffer().add(object);
    }

    // Method to stop sending data
    public void stopSending() {
        sender.stop();
    }

    // Method to start receiving data
    public void startReceiving() {
        try {
            receiver.receive();
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, ReliableMulticast.class, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to stop receiving data
    public void stopReceiving() {
        receiver.stop();
    }

    // Method to retrieve workable objects
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