package edu.rit.se.crashavoidance.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class WifiDirectHandler extends NonStopIntentService {

    public static final String androidServiceName = "WiFi Direct Handler";
    public static final String LOG_TAG = "wifiDirectHandler";
    private final IBinder binder = new WifiTesterBinder();

    private final String SERVICE_MAP_KEY = "serviceMapKey";

    private final String PEERS = "peers";

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;
    private WifiP2pDeviceList peers;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver receiver;
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pServiceRequest serviceRequest;

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
        super.onCreate();
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        channel = wifiP2pManager.initialize(this, getMainLooper(), null);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        receiver = new WifiDirectBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

  @Override
  public void onDestroy() {
    unregisterReceiver(receiver);
    super.onDestroy();
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
                Log.i(LOG_TAG, "Local service added");
                Log.d(LOG_TAG, "Local service added");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure adding local service: " + FailureReason.fromInteger(reason).toString());
            }
        });
    }

  /**
   * Starts discovering services. First registers DnsSdTxtRecordListener and a
   * DnsSdServiceResponseListener. Then adds a service request and begins to discover services. The
   * callbacks within the registered listeners are called when services are found.
   */
    public void startDiscoveringServices() {
        //Add listeners
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                Log.i(LOG_TAG, "DnsSDTxtRecord available");

                Intent intent = new Intent(Event.DNS_SD_TXT_RECORD_ADDED.toString());
                localBroadcastManager.sendBroadcast(intent);
                dnsSdTxtRecordMap.put(srcDevice.deviceAddress, new DnsSdTxtRecord(fullDomainName, txtRecordMap, srcDevice));
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener serviceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                // Not sure if we want to track the map here or just send the service in the request to let the caller do
                // what it wants with it
                Log.d(LOG_TAG, "Found service at address " + srcDevice.deviceAddress + " with name " + srcDevice.deviceName);
                dnsSdServiceMap.put(srcDevice.deviceAddress, new DnsSdService(instanceName, registrationType, srcDevice));
                Intent intent = new Intent(Event.DNS_SD_SERVICE_AVAILABLE.toString());
                intent.putExtra(SERVICE_MAP_KEY, srcDevice.deviceAddress);
                localBroadcastManager.sendBroadcast(intent);
            }
        };

        wifiP2pManager.setDnsSdResponseListeners(channel, serviceResponseListener, txtRecordListener);

        //Now that we are listening for services, begin to find them
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        //Tell the framework we want to scan for services. Prerequisite for discovering services
        wifiP2pManager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(LOG_TAG, "Service discovery request added");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure adding service discovery request: " + FailureReason.fromInteger(reason).toString());
            }
        });
        wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(LOG_TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure initiating service discovery: " + FailureReason.fromInteger(reason).toString());
            }
        });

    }

    public Map<String, DnsSdService> getDnsSdServiceMap(){
        return dnsSdServiceMap;
    }


    public String getPEERS() {
        return PEERS;
    }


    public String getSERVICE_MAP_KEY() {
        return SERVICE_MAP_KEY;
    }
    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

  /**
   * Removes a registered local service.
   */
    public void removeService() {
        wifiP2pManager.removeLocalService(channel, this.serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Intent intent = new Intent(Event.SERVICE_REMOVED.toString());
                localBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    private void requestPeers() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(LOG_TAG, "Discover peers successful");
                //No data about peers can be collected here.
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure discovering peers: " + FailureReason.fromInteger(reason).toString());
            }
        });
    }

  /**
   * Connects to a service
   * @param service The service to connect to.
   */
    public void connectToService(DnsSdService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.getSrcDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if(serviceRequest != null) {
            wifiP2pManager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(LOG_TAG, "Service request removed");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(LOG_TAG, "Failure removing service request: " + FailureReason.fromInteger(reason).toString());
                }
            });
        }

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(LOG_TAG, "Connected to service");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure connecting to service: " + FailureReason.fromInteger(reason).toString());
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
                        intent.putExtra(PEERS, peers);
                        localBroadcastManager.sendBroadcast(intent);
                    }
                });
            }
        }
    }

  /**
   * Toggle wifi
   * @param wifiEnabled whether or not wifi should be enabled
   */
    public void setWifiEnabled(boolean wifiEnabled) {
        wifiManager.setWifiEnabled(wifiEnabled);
    }

  /**
   * Allows for binding to the service.
   */
    public class WifiTesterBinder extends Binder {
        public WifiDirectHandler getService() {
            return WifiDirectHandler.this;
        }
    }

  /**
   * Actions that can be broadcasted by the handler
   */
    public enum Event {
        DNS_SD_TXT_RECORD_ADDED("dnsSdTxtRecordAdded"),
        DNS_SD_SERVICE_AVAILABLE("dnsSdServiceAvailable"),
        SERVICE_REMOVED("serviceRemoved"),
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
