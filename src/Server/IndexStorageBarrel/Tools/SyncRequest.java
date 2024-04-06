package Server.IndexStorageBarrel.Tools;

// General imports
import java.util.HashMap;
import java.io.Serializable;

/**
 * The SyncRequest class represents a request to synchronize data between
 * @param lastIDs the last IDs of the data to synchronize
 */
public record SyncRequest(HashMap<String, Integer> lastIDs) implements Serializable {
}
