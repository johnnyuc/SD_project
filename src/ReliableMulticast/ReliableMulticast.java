package ReliableMulticast;

import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Objects.SyncData;
import ReliableMulticast.Receiver.Receiver;
import ReliableMulticast.Receiver.ReceiverListener;

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

    public void stopProtocol() {
        System.out.println("Stopping receiving");
        receiver.stopReceiving();
        sender.getSendBuffer().add(ReceiverListener.POISON_PILL);
    }

    public Object getData() {
        try {
            Object data = receiver.getWorkerQueue().take();
            if (data == ReceiverListener.POISON_PILL){
                System.out.println("Got poisoned");
                return null;
            }

            return data;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}