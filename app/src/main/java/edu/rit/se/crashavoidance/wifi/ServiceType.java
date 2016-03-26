package edu.rit.se.crashavoidance.wifi;

public enum ServiceType {
    PRESENCE_TCP("_presence._tcp");

    private final String serviceType;

    private ServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String toString() {
        return serviceType;
    }
}
