package edu.rit.se.crashavoidance.wifi;

import android.net.wifi.p2p.WifiP2pDevice;

public class DnsSdService {
    String instanceName;
    String registrationType;
    WifiP2pDevice srcDevice;

    public DnsSdService(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
        this.instanceName = instanceName;
        this.registrationType = registrationType;
        this.srcDevice = srcDevice;
    }
}
