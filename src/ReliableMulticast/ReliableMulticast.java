package ReliableMulticast;

import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Receiver.Receiver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class ReliableMulticast {
    private final Sender sender;
    private final Receiver receiver;
    private Object[] senderBuffer;
    private Thread receiverThread;

    public ReliableMulticast(String multicastGroup, int port) {
        try {
            String senderIP = InetAddress.getLocalHost().getHostAddress();
            this.sender = new Sender(multicastGroup, port, senderIP);
            this.receiver = new Receiver(sender, multicastGroup, port);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO Adicionar o objeto a uma queue aqui e a thread sender que vai ser
    // começada dentro do reliable multicast vem buscar quando houver informação
    public void send(Object object) {
        sender.getSendBuffer().add(object);
    }

    public void startReceiving() {
        try {
            receiver.receive();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void stopReceiving() {
        System.out.println("Stopping receiving");
        receiver.stopReceiving();
    }

    public Object getData() {
        try {
            return receiver.getWorkerQueue().poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}