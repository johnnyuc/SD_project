package ReliableMulticast.Receiver;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

import ReliableMulticast.Objects.Container;

public class ReceiverWorker implements Runnable {

    // Listener to get data from
    private final ReceiverListener listener;

    // Map to store the received containers
    private final HashMap<String, Container[]> containersReceived;

    // Queue for whatever the worker needs
    private final BlockingQueue<Object> workerQueue;

    // Flag to control the worker's execution
    private volatile boolean running = true;

    ReceiverWorker(ReceiverListener listener, BlockingQueue<Object> workerQueue) {
        this.listener = listener;
        this.workerQueue = workerQueue;
        this.containersReceived = new HashMap<>();

        // In case of CTRL+C, set running to false
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
    }

    @Override
    public void run() {
        try {
            while (running) {
                Container receivedContainer = unpackContainer(listener.getDataFromQueue());
                System.out.println("Received packet " + (receivedContainer.getPacketNumber() + 1) + " of " + receivedContainer.getTotalPackets() + 
                " with UUID:" + receivedContainer.hashCode());
                addContainerToMap(receivedContainer);
                int[] missingContainers = findMissingContainers(receivedContainer);

                if(missingContainers.length > 0) {
                    for (int missingContainer : missingContainers) {
                        // TODO: Send retransmit request
                        System.out.println("Missing packet " + missingContainer + " of " + receivedContainer.getTotalPackets());
                    }
                    workerQueue.add(missingContainers);
                }
                else if (receivedContainer.isLastPacket()) {
                    workerQueue.add(reconstructData(receivedContainer.getDataID()));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
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

    private Object reconstructData(String dataID) throws IOException, ClassNotFoundException {
        // Buffer to assemble the data
        ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

        // Get the array of containers for the dataID
        Container[] containers = containersReceived.get(dataID);

        // Iterate the containers array
        for (Container container : containers) {
            // If a container is missing, break the loop
            if (container == null) {
                // TODO tirar este debug obviamente
                System.out.println("MUITA MERDA! NULL NA RECONSTRUÇÃO");
                break;
            }
            // Write the data of the container to the buffer
            dataBuffer.write(container.getData());
        }

        // Get the object
        ByteArrayInputStream compressedDataBis = new ByteArrayInputStream(dataBuffer.toByteArray());

        return deserializeData(compressedDataBis);
    }

    private static Object deserializeData(ByteArrayInputStream compressedMessageBis) throws IOException, ClassNotFoundException {
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
        ByteArrayInputStream dataBis = new ByteArrayInputStream(decompressedData);
        ObjectInputStream objectOis = new ObjectInputStream(dataBis);
        return objectOis.readObject();
    }
}
