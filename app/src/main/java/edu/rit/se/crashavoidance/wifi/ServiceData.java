package edu.rit.se.crashavoidance.wifi;

import java.util.HashMap;

/**
 *
 */
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

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public HashMap<String, String> getRecord() {
        return record;
    }

    public void setRecord(HashMap<String, String> record) {
        this.record = record;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
