package ReliableMulticast;

import ReliableMulticast.Receiver.ReceiverWorker;
import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Receiver.Receiver;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Time;
import java.util.concurrent.TimeUnit;

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
        sender.getSendBuffer().add(object);
    }

    public void stopSending() {
        sender.stop();
    }

    public void startReceiving() {
        try {
            receiver.receive();
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, LogUtil.logging.LOGGER, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopReceiving() {
        receiver.stop();
    }

    public Object getData() {
        try {
            Object data = receiver.getWorkerQueue().take();
            if (data == ReceiverWorker.STOP_PILL) {
                stopReceiving();
                return null;
            }
            return data;
        } catch (InterruptedException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, LogUtil.logging.LOGGER, e);
            return null;
        }
    }
}