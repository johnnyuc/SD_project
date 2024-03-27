package ReliableMulticast.Receiver;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.SynchronousQueue;
import java.util.zip.GZIPInputStream;

import ReliableMulticast.Objects.Container;

public class ReceiverWorker implements Runnable {

    // Listener to get data from
    private final ReceiverListener listener;

    // Map to store the received containers
    private final HashMap<String, Container[]> containersReceived;

    // Flag to control the worker's execution
    private volatile boolean running = true;

    ReceiverWorker(ReceiverListener listener, SynchronousQueue<byte[]> dataQueue) {
        this.listener = listener;
        this.containersReceived = new HashMap<>();

        // Start the worker thread
        new Thread(this).start();

        // In case of CTRL+C, set running to false
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
    }

    @Override
    public void run() {
        try {
            while (running) {
                Container receivedContainer = unpackContainer(listener.getDataFromQueue());
                addContainerToMap(receivedContainer);
                int[] missingContainers = findMissingContainers(receivedContainer);
                // If find missing containers = [] && container é ultimo, dados bons para usar
                if (missingContainers.length == 0 && receivedContainer.isLastPacket()) {
                    reconstructData(receivedContainer.getDataID());
                    // TODO: Adicionar reconstructedData a uma queue que depois quem esteja a usar
                    // este protocolo de reliable multicast possa ir buscar
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO: handle exception
            System.err.println("Error: " + e.getMessage());
        }
    }

    private Container unpackContainer(byte[] serializedContainer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedContainer);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Container) ois.readObject();
    }

    private void addContainerToMap(Container container) {
        // If the dataID of the container is new
        if (!containersReceived.containsKey(container.getDataID()))
            // Initialize the container array with the size determined by dataID
            containersReceived.put(container.getDataID(), new Container[container.getTotalPackets()]);

        // Add the container to the container array in the map at the specified index
        containersReceived.get(container.getDataID())[container.getPacketNumber()] = container;
    }

    //TODO: Podemos pensar em formas de memorizar se nao faltam mensagens para
    // tras. Por exemplo, memorizar que do 1 - 30 já estão todos. Assim encurtamos
    // este ciclo. Vale a pena?
    //TODO: Acho que se o ultimo container for perdido, não vai ser dado como
    // perdido. ver melhor sff
    private int[] findMissingContainers(Container currContainer) {
        Container[] containers = containersReceived.get(currContainer.getDataID());
        List<Integer> missingContainers = new ArrayList<>();

        // Iterate the received containers list until the given container
        for (int i = 0; i < currContainer.getPacketNumber(); i++)
            if (containers[i] == null)
                missingContainers.add(i);

        // Transform the List to an array
        int[] arr = new int[missingContainers.size()];
        for (int i = 0; i < missingContainers.size(); i++)
            arr[i] = missingContainers.get(i);

        return arr;
    }

    private Class<?> reconstructData(String dataID) throws IOException, ClassNotFoundException {
        // Checking if dataID is in the map
        if (!containersReceived.containsKey(dataID))
            return null;

        // Buffer to assemble the data
        ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

        // Get the array of containers for the dataID
        Container[] containers = containersReceived.get(dataID);

        // Iterate the containers array
        for (Container container : containers) {
            // If a container is missing, break the loop
            if (container == null)
                break;
            // Write the data of the container to the buffer
            dataBuffer.write(container.getData());
        }

        // Get the object
        ByteArrayInputStream compressedMessageBis = new ByteArrayInputStream(dataBuffer.toByteArray());

        return getMessage(compressedMessageBis);
    }

    private static Class<?> getMessage(ByteArrayInputStream compressedMessageBis) throws IOException, ClassNotFoundException {
        // Decompress the object
        GZIPInputStream gzipInputStream = new GZIPInputStream(compressedMessageBis);
        ByteArrayOutputStream decompressedByteStream = new ByteArrayOutputStream();
        byte[] decompressBuffer = new byte[1024];
        int bytesRead;

        // Read the decompressed object
        while ((bytesRead = gzipInputStream.read(decompressBuffer)) != -1) {
            decompressedByteStream.write(decompressBuffer, 0, bytesRead);
        }
        byte[] decompressedData = decompressedByteStream.toByteArray();

        // Deserialize the object
        ByteArrayInputStream messageBis = new ByteArrayInputStream(decompressedData);
        ObjectInputStream objectOis = new ObjectInputStream(messageBis);
        return (Class<?>) objectOis.readObject();
    }
}
