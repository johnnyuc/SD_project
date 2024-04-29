package ReliableMulticast.Receiver;

// Package imports
import ReliableMulticast.Objects.Container;

// Logging imports
import Logger.LogUtil;

// General imports
import java.util.HashMap;

/**
 * The IntegrityChecker class is responsible for checking the integrity of
 * received data and requesting retransmission if necessary.
 */
public class IntegrityChecker implements Runnable {
    /**
     * The ReceiverWorker object used to access the received data.
     */
    private final ReceiverWorker worker;
    /**
     * The period of time to check for timeouts in milliseconds.
     */
    private static final int TIMEOUT_CHECK_PERIOD = 5000;
    // This will keep track of how many retransmit requests were sent
    /**
     * A hash table that keeps track of how many retransmit requests were sent for
     */
    private final HashMap<String, Integer> retransmitSent = new HashMap<>();

    /**
     * Constructs an IntegrityChecker object with the specified ReceiverWorker.
     * 
     * @param worker the ReceiverWorker object
     */
    public IntegrityChecker(ReceiverWorker worker) {
        this.worker = worker;
        new Thread(this).start();
    }

    /**
     * Runs the integrity checking process in a separate thread.
     */
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
                LogUtil.logError(LogUtil.ANSI_WHITE, IntegrityChecker.class, e);
            }
        }
        LogUtil.logInfo(LogUtil.ANSI_CYAN, IntegrityChecker.class, "IntegrityChecker thread stopped");
    }

    /**
     * Checks if the data with the specified dataID has timed out and requests
     * retransmission if necessary.
     * 
     * @param dataID the ID of the data to be checked
     */
    private void checkTimedout(String dataID) {
        Integer retransmitNr = retransmitSent.get(dataID);

        // If no retransmit request was sent, create the entry in the hash table
        if (retransmitNr == null) {
            retransmitSent.put(dataID, 0);
            retransmitNr = 0;
        }
        // Discard the data if no answer is given in 3 retransmit requests
        else if (retransmitNr == 3) {
            retransmitSent.remove(dataID);
            LogUtil.logInfo(LogUtil.ANSI_WHITE, IntegrityChecker.class,
                    "Too many retransmit requests. Discarding data with ID: " + dataID);
            worker.getContainersReceived().remove(dataID);
            return;
        }

        // Get the timestamp of the container
        long timestamp = worker.getContainersReceived().get(dataID).getTimestamp();
        Container[] containers = worker.getContainersReceived().get(dataID).getContainers();

        // If the timestamp is older than 5 seconds
        if (System.currentTimeMillis() - timestamp > TIMEOUT_CHECK_PERIOD) {
            // Get the missing containers
            for (int i = 0; i < containers.length; i++) {
                if (worker.getContainersReceived().get(dataID).getContainers()[i] == null) {
                    worker.getSender().requestRetransmit(i, dataID);
                    retransmitSent.put(dataID, retransmitNr + 1);
                }
            }
        } else {
            retransmitSent.put(dataID, 0);
        }
    }
}