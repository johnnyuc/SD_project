package ReliableMulticast.Receiver;

import Logger.LogUtil;
import ReliableMulticast.Objects.Container;

public class IntegrityChecker implements Runnable {
    private final ReceiverWorker worker;
    private static final int TIMEOUT_CHECK_PERIOD = 5000;

    public IntegrityChecker(ReceiverWorker worker) {
        this.worker = worker;
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (worker.isRunning()) {
            // Call the checkTimedout function
            for (String dataID : worker.getContainersReceived().keySet())
                checkTimedout(dataID);
            // Sleep for a while
            try {
                Thread.sleep(TIMEOUT_CHECK_PERIOD);
            } catch (InterruptedException e) {
                // Handle interruption
            }
        }
        System.out.println("IntegrityChecker thread stopped");
    }

    private void checkTimedout(String dataID) {
        // Get the timestamp of the container
        long timestamp = worker.getContainersReceived().get(dataID).getTimestamp();
        Container[] containers = worker.getContainersReceived().get(dataID).getContainers();
        // If the timestamp is older than 5 seconds
        if (System.currentTimeMillis() - timestamp > TIMEOUT_CHECK_PERIOD) {
            // Get the missing containers
            for (int i = 0; i < containers.length; i++)
                if (worker.getContainersReceived().get(dataID).getContainers()[i] == null)
                    worker.getSender().requestRetransmit(i, dataID);
        }
    }
}