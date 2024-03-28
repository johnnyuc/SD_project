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

import ReliableMulticast.LogUtil;
import ReliableMulticast.Objects.Container;
import ReliableMulticast.Sender.Sender;

public class ReceiverWorker implements Runnable {
    // Sender to send retransmit requests
    private final Sender sender;

    // Listener to get data from
    private final ReceiverListener listener;

    // Map to store the received containers
    private final HashMap<String, Container[]> containersReceived = new HashMap<>();

    // Queue for whatever the worker needs
    private final BlockingQueue<Object> workerQueue;

    ReceiverWorker(Sender sender, ReceiverListener listener, BlockingQueue<Object> workerQueue) {
        this.sender = sender;
        this.listener = listener;
        this.workerQueue = workerQueue;
    }

    @Override
    public void run() {
        try {
            while(true)
                processContainer();
        } catch (IOException | ClassNotFoundException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
        }
    }

    private void processContainer() throws IOException, ClassNotFoundException {
        Object packedContainer = listener.getDataFromQueue();
        Container container = unpackContainer((byte[]) packedContainer);
        System.out.println("Received packet " + (container.getPacketNumber() + 1) + " of "
                + container.getTotalPackets());
        addContainerToMap(container);
        int[] missingContainers = findMissingContainers(container);
        if (missingContainers.length > 0) {
            sender.requestRetransmit(missingContainers, container.getDataID());
        } else if (container.isLastPacket()) {
            workerQueue.add(reconstructData(container.getDataID()));
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

    // TODO: Se forem perdidos os ultimos 10 containers, por exemplo, nunca mais vao ser encontrados. 
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
                // TODO: REMOVE THIS WHEN FIXED
                System.err.println("SHOULDN'T HAPPEN; RECONSTRUCT ERROR: Missing container!");
                break;
            }
            // Write the data of the container to the buffer
            dataBuffer.write(container.getData());
        }

        // Get the object
        ByteArrayInputStream compressedDataBis = new ByteArrayInputStream(dataBuffer.toByteArray());

        return deserializeData(compressedDataBis);
    }

    private static Object deserializeData(ByteArrayInputStream compressedMessageBis)
            throws IOException, ClassNotFoundException {
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
