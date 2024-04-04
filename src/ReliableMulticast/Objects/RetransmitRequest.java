package ReliableMulticast.Objects;

import java.io.Serializable;

/**
 * RetransmitData
 */
public record RetransmitRequest(int missingContainer, String dataID) implements Serializable {
}