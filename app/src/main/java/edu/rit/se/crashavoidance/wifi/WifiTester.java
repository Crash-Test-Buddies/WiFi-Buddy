package edu.rit.se.crashavoidance.wifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

import java.util.HashMap;
import java.util.Map;

public class WifiTester {

    //Variables provided by builder?
    private String serviceName;
    private ServiceType serviceType;
    private Map<String, String> userRecords;
    private int listenPort;

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;

    //Variables created in constructor
    WifiP2pManager.Channel channel;
    WifiP2pManager manager;

    private WifiTester(Builder builder) {
        this.serviceName = builder.serviceName;
        this.serviceType = builder.serviceType;
        this.userRecords = new HashMap<>(builder.record);
        this.listenPort = builder.listenPort;

        dnsSdTxtRecordMap = new HashMap<>();
        dnsSdServiceMap = new HashMap<>();
    }

    public void startAddingLocalService() {
        Map<String, String> records = new HashMap<String,String>(userRecords);
        records.put("listenport", Integer.toString(listenPort));
        records.put("available", "visible");

        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(serviceName,
                serviceType.toString(), records);

        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Nothing needed here, maybe just logs
            }

            @Override
            public void onFailure(int reason) {
                //Errors inside of WifiP2pManager
            }
        });
    }

    public void startDiscoveringServices() {
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                //Should probably log that a record is available

                //Record this data for later access?
                dnsSdTxtRecordMap.put(srcDevice.deviceAddress, new DnsSdTxtRecord(fullDomainName, txtRecordMap, srcDevice));
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener serviceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                dnsSdServiceMap.put(srcDevice.deviceAddress, new DnsSdService(instanceName, registrationType, srcDevice));
                //TODO: Maybe an observer pattern or something to indicate a change
            }
        };

        manager.setDnsSdResponseListeners(channel, serviceResponseListener, txtRecordListener);
    }

    public class Builder {
        protected String serviceName = "testService";
        protected ServiceType serviceType = ServiceType.PRESENCE_TCP;
        protected Map<String, String> record = new HashMap<>();
        protected int listenPort = 4545;

        public Builder() {}

        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }
        public void addRecord(String key, String value) { this.record.put(key, value); }
        public void setListenPort(int listenPort) { this.listenPort = listenPort; }


        public WifiTester build() {
            return new WifiTester(this);
        }
    }
}
