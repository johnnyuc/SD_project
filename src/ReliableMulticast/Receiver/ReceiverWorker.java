package ReliableMulticast.Receiver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ReliableMulticast.Objects.Container;

public class ReceiverWorker implements Runnable {
    private final ReceiverListener listener;
    private HashMap<String, Container[]> containersReceived;

    // Flag to control the worker's execution
    private volatile boolean running = true;

    ReceiverWorker() {
        // TODO: Se calhar é melhor ter uma classe mais acima, só Receiver, que vai
        // criar estas duas threads e passa o objeto ReceiverListener para este objeto

        listener = new ReceiverListener("224.0.0.1", 12345);
        // TODO: Pensar se vale a pena criar o hashmap com tamanho maior inicialmente.
        // Default é 16.
        containersReceived = new HashMap<String, Container[]>();

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
            e.printStackTrace();
        }
    }

    /**
     * Unpacks a serialized Container object from a byte array.
     * 
     * @param serializedContainer the byte array containing the serialized Container
     *                            object
     * @return the deserialized Container object
     * @throws IOException            if an I/O error occurs while reading the
     *                                serialized data
     * @throws ClassNotFoundException if the class of the serialized object cannot
     *                                be found
     */
    private Container unpackContainer(byte[] serializedContainer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedContainer);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Container receivedContainer = (Container) ois.readObject();
        return receivedContainer;
    }

    /**
     * Adds a Container object to the containersReceived map.
     * If the dataID of the container is new, initializes the container array with
     * the size determined by the container.
     * 
     * @param container the Container object to be added to the map
     */
    private void addContainerToMap(Container container) {
        // If the dataID of the container is new
        if (!containersReceived.containsKey(container.getDataID()))
            // Initialize the container array with the size determined by dataID
            containersReceived.put(container.getDataID(), new Container[container.getTotalPackets()]);

        // Add the container to the container array in the map at the specified index
        containersReceived.get(container.getDataID())[container.getPacketNumber()] = container;
    }

    // TODO: Podemos pensar em formas de memorizar se nao faltam mensagens para
    // tras. Por exemplo, memorizar que do 1 - 30 já estão todos. Assim encurtamos
    // este ciclo. Vale a pena?
    // TODO: Acho que se o ultimo container for perdido, não vai ser dado como
    // perdido. ver melhor sff
    /**
     * Finds the missing container packet numbers for the given container dataID.
     * 
     * @param currContainer the current container to find missing packets for
     * @return an array containing the packet numbers of the missing containers
     */
    private int[] findMissingContainers(Container currContainer) {
        Container[] containers = containersReceived.get(currContainer.getDataID());
        List<Integer> missingContainers = new ArrayList<Integer>();
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

    private void reconstructData(String dataID) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reconstructData'");
    }

}
