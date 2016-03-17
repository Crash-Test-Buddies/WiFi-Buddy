package edu.rit.se.crashavoidance;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by Brett on 2/2/2016.
 */
public class WiFiP2pService {
    public WifiP2pDevice device;
    public String instanceName = null;
    public String serviceRegistrationType = null;

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof WiFiP2pService)) {
            return false;
        } else {
            WiFiP2pService other = (WiFiP2pService) o;
            return other.device.deviceName.equals(this.device.deviceName)
                    && other.instanceName.equals(this.instanceName);
        }
    }
}
