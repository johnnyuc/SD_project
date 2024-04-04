package Server.IndexStorageBarrel.Tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncData(HashMap<String, List<Map<String, Object>>> tableResults) implements Serializable {
}
