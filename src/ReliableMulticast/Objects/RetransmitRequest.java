package ReliableMulticast.Objects;

/**
 * RetransmitData
 */
public class RetransmitRequest {
    private final int missingContainer;
    private final String dataID;

    public RetransmitRequest(int missingContainer, String dataID) {
        this.missingContainer = missingContainer;
        this.dataID = dataID;
    }

    // Getters
    public int getMissingContainer() {
        return missingContainer;
    }

    public String getDataID() {
        return dataID;
    }
}