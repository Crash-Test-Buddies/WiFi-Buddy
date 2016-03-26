package edu.rit.se.crashavoidance.wifi;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

public class DnsSdTxtRecord {
    String fullDomain;
    Map record;
    WifiP2pDevice device;

    public DnsSdTxtRecord(String fullDomain, Map record, WifiP2pDevice device) {
        this.fullDomain = fullDomain;
        this.record = record;
        this.device = device;
    }
}
