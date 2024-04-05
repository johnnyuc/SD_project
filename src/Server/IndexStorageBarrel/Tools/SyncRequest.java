package Server.IndexStorageBarrel.Tools;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Represents a synchronization request.
 * 
 * This class is used to send a request for synchronization between the server
 * and the client.
 * It contains the last known IDs of the synchronized data.
 */
public record SyncRequest(HashMap<String, Integer> lastIDs) implements Serializable {
}
