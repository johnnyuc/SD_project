package Server.IndexStorageBarrel.Tools;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.HashMap;

public record SyncData(HashMap<String, ResultSet> tableResults) implements Serializable {
}
