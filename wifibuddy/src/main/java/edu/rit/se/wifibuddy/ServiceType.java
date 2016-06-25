package edu.rit.se.wifibuddy;

public enum ServiceType {

    PRESENCE_TCP("_presence._tcp");

    private final String serviceType;

    ServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String toString() {
        return serviceType;
    }
}
