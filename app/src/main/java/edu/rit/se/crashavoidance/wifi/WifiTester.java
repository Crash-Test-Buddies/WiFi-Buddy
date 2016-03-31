package edu.rit.se.crashavoidance.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

public class WifiTester extends BroadcastReceiver {

    private String serviceName;
    private ServiceType serviceType;
    private Map<String, String> userRecords;
    private int listenPort;
    private Context context;

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;
    private WifiP2pDeviceList peers;
    private LocalBroadcastManager localBroadcastManager;

    //Variables created in constructor
    private WifiP2pManager.Channel channel;
    private WifiP2pManager manager;


    private WifiTester(Builder builder) {
        this.serviceName = builder.serviceName;
        this.serviceType = builder.serviceType;
        this.userRecords = new HashMap<>(builder.record);
        this.listenPort = builder.listenPort;
        this.context = builder.context;

        dnsSdTxtRecordMap = new HashMap<>();
        dnsSdServiceMap = new HashMap<>();
        peers = new WifiP2pDeviceList();

        localBroadcastManager = LocalBroadcastManager.getInstance(this.context);
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

                Intent intent = new Intent(Event.DNS_SD_TXT_RECORD_ADDED.toString());
                localBroadcastManager.sendBroadcast(intent);
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

    private void requestPeers() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //TODO: Log that this was successful.
                //No data about peers can be collected here.
            }

            @Override
            public void onFailure(int reason) {
                //TODO: log why this failed
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if(manager != null) {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        WifiTester.this.peers = peers;
                    }
                });
            }
        }
    }



    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public void setChannel(WifiP2pManager.Channel channel) {
        this.channel = channel;
    }

    public WifiP2pManager getManager() {
        return manager;
    }

    public void setManager(WifiP2pManager manager) {
        this.manager = manager;
    }

    public static class Builder {
        protected String serviceName = "testService";
        protected ServiceType serviceType = ServiceType.PRESENCE_TCP;
        protected Map<String, String> record = new HashMap<>();
        protected int listenPort = 4545;
        protected Context context;

        private WifiTester wifiInstance;
        private static Builder builderInstance;

        private Builder() {}

        public Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        public Builder setServiceType(ServiceType serviceType) {
            this.serviceType = serviceType;
            return this;
        }
        public Builder addRecord(String key, String value) {
            this.record.put(key, value);
            return this;
        }
        public Builder setListenPort(int listenPort) {
            this.listenPort = listenPort;
            return this;
        }

        public WifiTester build(Context context) {
            if (wifiInstance == null){
                wifiInstance = new WifiTester(this);
            }
            return wifiInstance;
        }

        public static Builder getInstance(){
            if (builderInstance == null){
                builderInstance = new Builder();
            }
            return builderInstance;
        }
    }

    public enum Event {
        DNS_SD_TXT_RECORD_ADDED("dnsSdTxtRecordAdded");

        private String eventName;

        private Event(String eventName) {
            this.eventName = eventName;
        }

        @Override
        public String toString() {
            return eventName;
        }
    }
}
