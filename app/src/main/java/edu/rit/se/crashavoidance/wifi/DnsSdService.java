package edu.rit.se.crashavoidance.wifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class DnsSdService implements Parcelable{
    private String instanceName;
    private String registrationType;
    private WifiP2pDevice srcDevice;

    public DnsSdService(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
        this.instanceName = instanceName;
        this.registrationType = registrationType;
        this.srcDevice = srcDevice;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getRegistrationType() {
        return registrationType;
    }

    public WifiP2pDevice getSrcDevice() {
        return srcDevice;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write Service record to parcelable
     * @param dest InstanceName, RegistrationType, then SrcDevice object
     * @param flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(instanceName);
        dest.writeString(registrationType);
        dest.writeValue(srcDevice);
    }
}
