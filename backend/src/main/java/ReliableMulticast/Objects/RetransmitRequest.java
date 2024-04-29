package ReliableMulticast.Objects;

// General imports
import java.io.Serializable;

/**
 * Represents a retransmit request for a missing container in a reliable
 * multicast protocol.
 * <p>
 * This class is used to request the retransmission of a specific container
 * identified by its dataID.
 * The missingContainer field indicates the index of the missing container in
 * the multicast stream.
 * 
 * @param missingContainer The index of the missing container in the multicast
 *                         stream.
 * @param dataID           The identifier of the data to be retransmitted.
 */
public record RetransmitRequest(int missingContainer, String dataID) implements Serializable {
}