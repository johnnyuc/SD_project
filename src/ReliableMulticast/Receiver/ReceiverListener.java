package ReliableMulticast.Receiver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.concurrent.SynchronousQueue;
import java.util.zip.GZIPInputStream;

import ReliableMulticast.Objects.Container;
import Server.Common.Message;

public class ReceiverListener implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 256;

    // Flag to control the listener's execution
    private volatile boolean running = true;

    // Socket
    private MulticastSocket socket;

    // Queue for worker to get data from
    private SynchronousQueue<byte[]> dataQueue;

    /**
     * Constructs a ReceiverListener object for receiving data packets via
     * multicast.
     * Joins the specified multicast group and starts a new thread for listening to
     * incoming packets.
     * Registers a shutdown hook to set the running flag to false when the program
     * is terminated.
     *
     * @param multicastGroup the multicast group to join
     * @param port           the port to listen on
     */
    public ReceiverListener(String multicastGroup, int port) {
        try {
            joinMulticastGroup(multicastGroup, port);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new Thread(this).start();
        // In case of CTRL+C, set running to false
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
    }

    /**
     * Joins the specified multicast group on the given port.
     *
     * @param multicastGroup the multicast group to join
     * @param port           the port to listen on
     * @throws IOException if an I/O error occurs while joining the multicast group
     */
    private void joinMulticastGroup(String multicastGroup, int port) throws IOException {
        this.socket = new MulticastSocket(port);
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(multicastGroup), port);
        socket.joinGroup(socketAddress, networkInterface);
    }

    @Override
    public void run() {
        try {
            while (running)
                dataQueue.add(receivePacket());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            running = false;
            socket.close();
        }
    }

    /**
     * Receives a packet from the multicast socket.
     *
     * @return the byte array containing the received packet data
     * @throws IOException if an I/O error occurs while receiving the packet
     */
    private byte[] receivePacket() throws IOException {
        byte[] buffer = new byte[MAX_PACKET_SIZE + MAX_PACKET_OVERHEAD];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        // TODO: Pensar se damos trim ao buffer
        // byte[] data = Arrays.copyOf(buffer, packet.getLength());
        return packet.getData();
    }

    /**
     * Retrieves data from the queue when available.
     * This method blocks until data is available in the queue.
     * 
     * @return the byte array containing the received data
     *         or null if interrupted while waiting for data
     */
    public byte[] getDataFromQueue() {
        try {
            // Retrieve data from the queue when available
            return dataQueue.take();
        } catch (InterruptedException e) {
            // TODO: Handle interruption
            e.printStackTrace();
            return null;
        }
    }

    // ============================================================================================================
    // ============================================================================================================
    // ============================================================================================================
    // Method to receive packets
    public Message receiveMessages() {
        ByteArrayOutputStream unpackingBuffer = new ByteArrayOutputStream();
        Message message;
        try {
            // Receive packets
            while (true) {

                // Unpack the packet
                ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
                ObjectInputStream ois = new ObjectInputStream(bis);
                Container receivedPacket = (Container) ois.readObject();
                unpackingBuffer.write(receivedPacket.getData());
                System.out.println("Received packet " + (receivedPacket.getPacketNumber() + 1) + " of "
                        + receivedPacket.getTotalPackets() + " with size: " + receivedPacket.getData().length
                        + " bytes");

                if (receivedPacket.isLastPacket()) {
                    ByteArrayInputStream compressedMessageBis = new ByteArrayInputStream(unpackingBuffer.toByteArray());
                    message = getMessage(compressedMessageBis);

                    /*
                     * DEBUGUS PURPUSUS
                     * System.out.println("URL: " + message.url);
                     * System.out.println("Title: " + message.title);
                     * System.out.println("Description: " + message.description);
                     * System.out.println("Tokens: " + message.tokens);
                     * System.out.println("URL Strings: " + message.urlStrings);
                     */

                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    // Method to close the socket
    public void close() {
        socket.close();
    }

    // Method to get a Message object from a compressed byte stream
    private static Message getMessage(ByteArrayInputStream compressedMessageBis)
            throws IOException, ClassNotFoundException {
        // Decompress the message
        GZIPInputStream gzipInputStream = new GZIPInputStream(compressedMessageBis);
        ByteArrayOutputStream decompressedMessageByteStream = new ByteArrayOutputStream();
        byte[] decompressBuffer = new byte[1024];
        int bytesRead;

        // Read the decompressed message
        while ((bytesRead = gzipInputStream.read(decompressBuffer)) != -1) {
            decompressedMessageByteStream.write(decompressBuffer, 0, bytesRead);
        }
        byte[] decompressedMessageData = decompressedMessageByteStream.toByteArray();

        // Deserialize the message
        ByteArrayInputStream messageBis = new ByteArrayInputStream(decompressedMessageData);
        ObjectInputStream messageOis = new ObjectInputStream(messageBis);
        return (Message) messageOis.readObject();
    }
}
