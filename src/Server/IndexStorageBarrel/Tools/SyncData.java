
package Server.IndexStorageBarrel.Tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the synchronization data for a table.
 * This class is used to store the results of a table synchronization operation.
 */
public record SyncData(HashMap<String, List<Map<String, Object>>> tableResults) implements Serializable {
}
