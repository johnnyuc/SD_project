package Server.IndexStorageBarrel.Tools;

import java.io.Serializable;
import java.util.HashMap;

public record SyncRequest(HashMap<String, Integer> lastIDs) implements Serializable {
}
