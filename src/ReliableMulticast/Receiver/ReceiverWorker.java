package ReliableMulticast.Receiver;

// Multicast imports
import ReliableMulticast.LogUtil;
import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Objects.Container;

// General imports
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

// Error imports
import java.io.IOException;

public class ReceiverWorker implements Runnable {
    // Multicast main objects
    // Sender
    private final Sender sender;
    // ReceiverListener
    private final ReceiverListener receiverListener;

    // Map to store the received containers
    private final HashMap<String, Container[]> containersReceived = new HashMap<>();

    // Queue for clean final data
    private final BlockingQueue<Object> workerQueue;

    // Running flag
    private volatile boolean running = true;

    // Stopping the thread
    public static final Object STOP_PILL = new Object();

    // Constructor
    ReceiverWorker(Sender sender, ReceiverListener listener, BlockingQueue<Object> workerQueue) {
        this.sender = sender;
        this.receiverListener = listener;
        this.workerQueue = workerQueue;
    }

    // Thread startup
    @Override
    public void run() {
        try {
            while (running)
                processContainer();
        } catch (IOException | ClassNotFoundException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, LogUtil.logging.LOGGER, e);
        }

        LogUtil.logInfo(LogUtil.ANSI_CYAN, LogUtil.logging.LOGGER, "ReceiverWorker thread stopped");
    }

    // Method to process a container
    private void processContainer() throws IOException, ClassNotFoundException {
        Object data = receiverListener.getData();

        // Check if the data is a stopping pill
        if (data == STOP_PILL) {
            running = false;
            return;
        }

        // Unpack the container
        byte[] packedContainer = (byte[]) data;
        Container container = unpackContainer(packedContainer);
        System.out.println("Received packet " + (container.getPacketNumber() + 1) + " of "
                + container.getTotalPackets());

        // Add the container to the map
        addContainerToMap(container);
        int[] missingContainers = findMissingContainers(container);

        // If there are missing containers send a retransmit request
        if (missingContainers.length > 0) {
            sender.requestRetransmit(missingContainers, container.getDataID());
        } else if (container.isLastPacket()) {
            workerQueue.add(reconstructData(container.getDataID()));
        }
    }

    // Method to unpack a container
    private Container unpackContainer(byte[] serializedContainer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedContainer);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Container) ois.readObject();
    }

    // Method to add a container to the map
    private void addContainerToMap(Container container) {
        // If the dataID of the container is new
        if (!containersReceived.containsKey(container.getDataID()))
            // Initialize the container array with the size determined by dataID
            containersReceived.put(container.getDataID(), new Container[container.getTotalPackets()]);

        // Add the container to the container array in the map at the specified index
        containersReceived.get(container.getDataID())[container.getPacketNumber()] = container;
    }

    // TODO: IF THE LAST CONTAINER IS LOST, THE DATA WILL NEVER BE RECONSTRUCTED. SAME FOR LAST X CONTAINERS
    // Method to find missing containers
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

    // Method to reconstruct the data
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

    // Method to deserialize the data
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

    // Method to stop the thread
    public void stop() {
        // Adding stopping pills to the queues
        receiverListener.putData(STOP_PILL);
        workerQueue.add(STOP_PILL);
    }
}
