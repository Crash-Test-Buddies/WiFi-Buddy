package edu.rit.se.wifibuddy;

import java.util.HashMap;

// TODO: Add JavaDoc
public class ServiceData {

    private String serviceName;
    private int port;
    private HashMap<String, String> record;
    private ServiceType serviceType;

    public ServiceData(String serviceName, int port, HashMap<String, String> record, ServiceType serviceType) {
        this.serviceName = serviceName;
        this.port = port;
        this.record = record;
        this.serviceType = serviceType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getPort() {
        return port;
    }

    public HashMap<String, String> getRecord() {
        return record;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    @Override
    public String toString() {
        return "- Service name: " + serviceName
            + "\n- Service port: " + port
            + "\n- Service record: " + record
            + "\n- Service type: " + serviceType;
    }
}
