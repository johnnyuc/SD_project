package ReliableMulticast.Objects;

/**
 * RetransmitData
 */
public class RetransmitRequest {
    private final int[] missingContainers;
    private final String dataID;

    public RetransmitRequest(int[] missingContainers, String dataID){
        this.missingContainers = missingContainers;
        this.dataID = dataID;
    }
    
    public int[] getMissingContainers(){
        return missingContainers;
    }

    public String getDataID(){
        return dataID;
    }
}