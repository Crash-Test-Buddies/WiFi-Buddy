package edu.rit.se.wifibuddy;

/**
 * An enum for translating P2P error codes to readable Strings.
 */
public enum FailureReason {

    ERROR("ERROR"),
    P2P_UNSUPPORTED("P2P UNSUPPORTED"),
    BUSY("BUSY"),
    NO_SERVICE_REQUESTS("NO SERVICE REQUESTS");

    private static FailureReason[] values;
    private final String reason;

    FailureReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return reason;
    }

    public static FailureReason fromInteger(int value) {
        if(values == null) {
            values = FailureReason.values();
        }
        return values[value];
    }
}
