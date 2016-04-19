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
import java.util.Timer;
import java.util.TimerTask;

/**
 * TODO add comment
 */
public class WifiDirectHandler extends NonStopIntentService {

    public static final String androidServiceName = "WiFi Direct Handler";
    public static final String LOG_TAG = "wifiDirectHandler";
    private final IBinder binder = new WifiTesterBinder();

    private final String SERVICE_MAP_KEY = "serviceMapKey";
    private final String PEERS_KEY = "peersKey";


    private final String PEERS = "peers";

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;
    private Map<String, Timer> serviceRemovalTask;
    private WifiP2pDeviceList peers;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver receiver;
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pServiceRequest serviceRequest;

    // Variables created in constructor
    private WifiP2pManager.Channel channel;
    private WifiP2pManager wifiP2pManager;
    private WifiManager wifiManager;

    // WifiDirectHandler logs
    private String logs = "";

    public WifiDirectHandler() {
        super(androidServiceName);
        dnsSdTxtRecordMap = new HashMap<>();
        dnsSdServiceMap = new HashMap<>();
        serviceRemovalTask = new HashMap<String, Timer>();
        peers = new WifiP2pDeviceList();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logMessage("WifiDirectHandler created");

        // Register the app with Wi-Fi Direct
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        logMessage("App registered with Wi-Fi Direct");

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Registers a WifiDirectBroadcastReceiver with an IntentFilter
        receiver = new WifiDirectBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
        logMessage("BroadcastReceiver registered");
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public void startAddingLocalService(ServiceData serviceData) {
        Map<String, String> records = new HashMap<>(serviceData.getRecord());
        records.put("listenport", Integer.toString(serviceData.getPort()));
        records.put("available", "visible");

        // Removes service if it is already added for some reason
        if (serviceInfo != null) {
            removeService();
        }

        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            serviceData.getServiceName(),
            serviceData.getServiceType().toString(),
            records
        );

        wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                logMessage("Local service added");
            }

            @Override
            public void onFailure(int reason) {
                logError("Failure adding local service: " + FailureReason.fromInteger(reason).toString());
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
                logMessage("DnsSDTxtRecord available");

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
                logMessage("Found service at address " + srcDevice.deviceAddress + " with name " + srcDevice.deviceName);
                DnsSdService service = new DnsSdService(instanceName, registrationType, srcDevice);
                String deviceAddress = srcDevice.deviceAddress;
                // TODO refactor this into a new method
                // Find out if this is a new service or one we have already discovered to determine
                // if we need to cancel a previously started timer task
                boolean newService = !dnsSdServiceMap.containsKey(deviceAddress);
                dnsSdServiceMap.put(deviceAddress, service);
                Intent intent = new Intent(Event.DNS_SD_SERVICE_AVAILABLE.toString());
                intent.putExtra(SERVICE_MAP_KEY, deviceAddress);
                localBroadcastManager.sendBroadcast(intent);
                // If this is a service we have previously discovered cancel the removal task
                // and we will add a new one
                if (!newService){
                    Log.d(LOG_TAG, "Cancelling removal request for device " + srcDevice.deviceName);
                    serviceRemovalTask.get(deviceAddress).cancel();
                }
                // Set a timer to remove the service if it is not discovered again before
                // the specified number of seconds
                serviceRemovalTask.put(deviceAddress, setServiceRemovalTimer(service, 10));
            }
        };

        wifiP2pManager.setDnsSdResponseListeners(channel, serviceResponseListener, txtRecordListener);

        // Now that we are listening for services, begin to find them
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        // Tell the framework we want to scan for services. Prerequisite for discovering services
        wifiP2pManager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                logMessage("Service discovery request added");
            }

            @Override
            public void onFailure(int reason) {
               logError("Failure adding service discovery request: " + FailureReason.fromInteger(reason).toString());
            }
        });

        wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                logMessage("Service discovery initiated");
            }

            @Override
            public void onFailure(int reason) {
                logError("Failure initiating service discovery: " + FailureReason.fromInteger(reason).toString());
            }
        });

    }

    public Timer setServiceRemovalTimer(DnsSdService service, int seconds){
        ServiceRemoveTask removeTask = new ServiceRemoveTask(service);
        Timer timer = new Timer();
        Log.d(LOG_TAG, "Setting removal task for device " + service.getSrcDevice().deviceName);
        timer.schedule(removeTask, seconds * 1000);
        return timer;
    }

    public Map<String, DnsSdService> getDnsSdServiceMap(){
        return dnsSdServiceMap;
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
        wifiP2pManager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                serviceInfo = null;
                Intent intent = new Intent(Event.SERVICE_REMOVED.toString());
                localBroadcastManager.sendBroadcast(intent);
                logMessage("Local service removed");
            }

            @Override
            public void onFailure(int reason) {
                logError("Failure removing local service: " + FailureReason.fromInteger(reason).toString());
            }
        });
    }

    public void requestPeers() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // No data about peers can be collected here.
                logMessage("Discover peers successful");
            }

            @Override
            public void onFailure(int reason) {
                logError("Failure discovering peers: " + FailureReason.fromInteger(reason).toString());
            }
        });
    }

  /**
   * Connects to a service
   * @param service The service to connect to
   */
    public void connectToService(DnsSdService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.getSrcDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if(serviceRequest != null) {
            wifiP2pManager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    logMessage("Service request removed");
                }

                @Override
                public void onFailure(int reason) {
                    logError("Failure removing service request: " + FailureReason.fromInteger(reason).toString());
                }
            });
        }

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                logMessage("Connected to service");
            }

            @Override
            public void onFailure(int reason) {
                logError("Failure connecting to service: " + FailureReason.fromInteger(reason).toString());
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
        if (wifiEnabled) {
            logMessage("Wi-Fi enabled");
        } else {
            logMessage("Wi-Fi disabled");
        }
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
        PEERS_CHANGED("peersChanged"),
        DEVICE_SERVICE_REMOVED("deviceServiceRemoved");

        private String eventName;

        Event(String eventName) {
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

    /**
     * Logs a message to the WifiDirectHandler logs and logs an info message to logcat
     * @param message Info message to log
     */
    public void logMessage(String message) {
        logs += message + "\n";
        Log.i(LOG_TAG, message);
    }

    /**
     * Logs a message to the WifiDirectHandler logs and logs an error message to logcat
     * @param message Error message to log
     */
    public void logError(String message) {
        logs += message + "\n";
        Log.e(LOG_TAG, message);
    }

    public String getPEERS() {
        return PEERS;
    }

    /**
     * Getter for the WifiDirectHandler logs
     * @return WifiDirectHandler logs
     */
    public String getLogs() {
        return logs;
    }

    class ServiceRemoveTask extends TimerTask {
        DnsSdService service;
        public ServiceRemoveTask(DnsSdService service){
            this.service = service;
        }
        public void run() {
            Log.d(LOG_TAG, "Removing " + service.getSrcDevice().deviceName + " from device map");
            dnsSdServiceMap.remove(service);
            Intent intent = new Intent(Event.DEVICE_SERVICE_REMOVED.toString());
            intent.putExtra(SERVICE_MAP_KEY, service.getSrcDevice().deviceAddress);
            localBroadcastManager.sendBroadcast(intent);
        }
    }
}
