package ReliableMulticast;

import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Receiver.Receiver;

import java.io.IOException;
import java.net.InetAddress;

public class ReliableMulticast {
    private final Sender sender;
    private final Receiver receiver;

    public ReliableMulticast(String multicastGroup, int port) {
        try {
            String senderIP = InetAddress.getLocalHost().getHostAddress();
            this.sender = new Sender(multicastGroup, port, senderIP);
            this.receiver = new Receiver(sender, multicastGroup, port);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(Object object) {
        sender.send(object);
    }

    public void startReceiving() {
        try {
            receiver.receive();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        }
    }

    public void stopReceiving() {
        receiver.stopReceiving();
    }
}