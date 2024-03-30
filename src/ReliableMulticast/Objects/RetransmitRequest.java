package ReliableMulticast.Objects;

/**
 * RetransmitData
 */
public record RetransmitRequest(int missingContainer, String dataID) {}