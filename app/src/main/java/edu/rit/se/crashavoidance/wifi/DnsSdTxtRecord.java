package edu.rit.se.crashavoidance.wifi;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

public class DnsSdTxtRecord {
    private String fullDomain;
    private Map record;
    private WifiP2pDevice device;

    public DnsSdTxtRecord(String fullDomain, Map record, WifiP2pDevice device) {
        this.fullDomain = fullDomain;
        this.record = record;
        this.device = device;
    }

    public String getFullDomain() {
        return fullDomain;
    }

    public Map getRecord() {
        return record;
    }

    public WifiP2pDevice getDevice() {
        return device;
    }
}
