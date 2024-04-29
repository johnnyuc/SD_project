package Server.IndexStorageBarrel.Tools;

// General imports
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.io.Serializable;

/**
 * The SyncData class represents the data to synchronize between IndexStorageBarrel and ReliableMulticast.
 * @param tableResults the results of the synchronization
 */
public record SyncData(HashMap<String, List<Map<String, Object>>> tableResults) implements Serializable {
}
