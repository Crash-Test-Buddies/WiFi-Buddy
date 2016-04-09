package edu.rit.se.crashavoidance.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

public class WifiDirectHandler extends NonStopIntentService {

    public static final String androidServiceName = "WifiTester";

    private final IBinder binder = new WifiTesterBinder();

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;
    private WifiP2pDeviceList peers;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver receiver;
    private WifiP2pServiceInfo serviceInfo;

    //Variables created in constructor
    private WifiP2pManager.Channel channel;
    private WifiP2pManager wifiP2pManager;
    private WifiManager wifiManager;

    public WifiDirectHandler() {
        super(androidServiceName);

        dnsSdTxtRecordMap = new HashMap<>();
        dnsSdServiceMap = new HashMap<>();
        peers = new WifiP2pDeviceList();
    }

    @Override
    public void onCreate() {
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        receiver = new WifiDirectBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        //TODO: Should this receiver be optional?
        registerReceiver(receiver, filter);
    }

    public void startAddingLocalService(ServiceData serviceData) {
        Map<String, String> records = new HashMap<String,String>(serviceData.getRecord());
        records.put("listenport", Integer.toString(serviceData.getPort()));
        records.put("available", "visible");

        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                serviceData.getServiceName(),
                serviceData.getServiceType().toString(),
                records
        );

        wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
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
                Intent intent = new Intent(Event.DNS_SD_SERVICE_AVAILABLE.toString());
                localBroadcastManager.sendBroadcast(intent);
            }
        };

        wifiP2pManager.setDnsSdResponseListeners(channel, serviceResponseListener, txtRecordListener);
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    public void removeService() {

    }

    private void requestPeers() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if(wifiP2pManager != null) {
                wifiP2pManager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        WifiDirectHandler.this.peers = peers;
                        Intent intent = new Intent(Event.PEERS_CHANGED.toString());
                        localBroadcastManager.sendBroadcast(intent);
                    }
                });
            }
        }
    }

    public void setWifiEnabled(boolean wifiEnabled) {
        wifiManager.setWifiEnabled(wifiEnabled);
    }

    public class WifiTesterBinder extends Binder {
        public WifiDirectHandler getService() {
            return WifiDirectHandler.this;
        }
    }

    public enum Event {
        DNS_SD_TXT_RECORD_ADDED("dnsSdTxtRecordAdded"),
        DNS_SD_SERVICE_AVAILABLE("dnsSdServiceAvailable"),
        PEERS_CHANGED("peersChanged");

        private String eventName;

        private Event(String eventName) {
            this.eventName = eventName;
        }

        @Override
        public String toString() {
            return eventName;
        }
    }

    private class WifiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            onHandleIntent(intent);
        }
    }
}
