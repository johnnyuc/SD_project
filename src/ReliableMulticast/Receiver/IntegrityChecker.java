package ReliableMulticast.Receiver;

import java.util.HashMap;

import Logger.LogUtil;
import ReliableMulticast.Objects.Container;

/* TODO: Apareceu este erro: Exception in thread "Thread-1" java.util.ConcurrentModificationException
at java.base/java.util.HashMap$HashIterator.nextNode(HashMap.java:1597)
at java.base/java.util.HashMap$KeyIterator.next(HashMap.java:1620)
at ReliableMulticast.Receiver.IntegrityChecker.run(IntegrityChecker.java:23)
at java.base/java.lang.Thread.run(Thread.java:840)
 */
public class IntegrityChecker implements Runnable {
    private final ReceiverWorker worker;
    private static final int TIMEOUT_CHECK_PERIOD = 5000;
    // This will keep track of how many retransmit requests were sent
    private final HashMap<String, Integer> retransmitSent = new HashMap<>();

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
                LogUtil.logError(LogUtil.ANSI_WHITE, IntegrityChecker.class, e);
            }
        }
        LogUtil.logInfo(LogUtil.ANSI_WHITE, IntegrityChecker.class, "IntegrityChecker thread stopped");
    }

    private void checkTimedout(String dataID) {
        Integer retransmitNr = retransmitSent.get(dataID);

        // If no retransmit request was sent, create the entry in the hash table
        if (retransmitNr == null) {
            retransmitSent.put(dataID, 0);
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