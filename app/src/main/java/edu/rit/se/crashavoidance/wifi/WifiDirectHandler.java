package edu.rit.se.crashavoidance.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO add comment
 */
public class WifiDirectHandler extends NonStopIntentService {

    public static final String androidServiceName = "Wi-Fi Direct Handler";
    public static final String LOG_TAG = "wifiDirectHandler";
    private final IBinder binder = new WifiTesterBinder();

    public static final String SERVICE_MAP_KEY = "serviceMapKey";
    private final String PEERS = "peers";

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;
    private WifiP2pDeviceList peers;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver receiver;
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pServiceRequest serviceRequest;

    // Flag for creating a no prompt service
    private boolean isCreatingNoPrompt = false;
    private ServiceData noPromptServiceData;

    // Variables created in onCreate()
    private WifiP2pManager.Channel channel;
    private WifiP2pManager wifiP2pManager;
    private WifiManager wifiManager;

    private IntentFilter filter;

    /** Constructor **/
    public WifiDirectHandler() {
        super(androidServiceName);
        dnsSdTxtRecordMap = new HashMap<>();
        dnsSdServiceMap = new HashMap<>();
        peers = new WifiP2pDeviceList();
    }

    /**
     * Registers the app with the Wi-Fi P2P framework and registers a WifiDirectBroadcastReceiver
     * with an IntentFilter that listens for Wi-Fi P2P Actions
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "WifiDirectHandler created");

        // Manages Wi-Fi P2P connectivity
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);

        // Manages Wi-Fi connectivity
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // initialize() registers the app with the Wi-Fi P2P framework
        // Channel is used to communicate with the Wi-Fi P2P framework
        // Main Looper is the Looper for the main thread of the current process
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        Log.i(LOG_TAG, "App registered with Wi-Fi P2P framework");

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Registers a WifiDirectBroadcastReceiver with an IntentFilter listening for P2P Actions
        receiver = new WifiDirectBroadcastReceiver();
        filter = new IntentFilter();

        // Indicates a change in the list of available peers
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates a change in the Wi-Fi P2P status
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        registerReceiver(receiver, filter);
        Log.i(LOG_TAG, "BroadcastReceiver registered");
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        Log.i(LOG_TAG, "BroadcastReceiver unregistered");
        super.onDestroy();
    }

    /**
     * // TODO add comment
     * @param serviceData
     */
    public void startAddingLocalService(ServiceData serviceData) {
        Map<String, String> records = new HashMap<>(serviceData.getRecord());
        records.put("listenport", Integer.toString(serviceData.getPort()));
        records.put("available", "visible");

        Log.i(LOG_TAG, "Adding local service " + serviceData);

        // Removes service if it is already added for some reason
        if (serviceInfo != null) {
            removeService();
        }

        // Service information
        // Instance name, service type, records map
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            serviceData.getServiceName(),
            serviceData.getServiceType().toString(),
            records
        );

        // Add the local service
        wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(LOG_TAG, "Local service added");
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
        // DnsSdTxtRecordListener
        // Interface for callback invocation when Bonjour TXT record is available for a service
        // Used to listen for incoming records and get peer device information
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                // Records of peer are available
                Log.i(LOG_TAG, "DnsSDTxtRecord available");

                Intent intent = new Intent(Action.DNS_SD_TXT_RECORD_ADDED);
                localBroadcastManager.sendBroadcast(intent);
                dnsSdTxtRecordMap.put(srcDevice.deviceAddress, new DnsSdTxtRecord(fullDomainName, txtRecordMap, srcDevice));
            }
        };

        // DnsSdServiceResponseListener
        // Interface for callback invocation when Bonjour service discovery response is received
        // Used to get service information
        WifiP2pManager.DnsSdServiceResponseListener serviceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                // Not sure if we want to track the map here or just send the service in the request to let the caller do
                // what it wants with it
                Log.i(LOG_TAG, "Found service at address " + srcDevice.deviceAddress + " with name " + srcDevice.deviceName);
                dnsSdServiceMap.put(srcDevice.deviceAddress, new DnsSdService(instanceName, registrationType, srcDevice));
                Intent intent = new Intent(Action.DNS_SD_SERVICE_AVAILABLE);
                intent.putExtra(SERVICE_MAP_KEY, srcDevice.deviceAddress);
                localBroadcastManager.sendBroadcast(intent);
            }
        };

        wifiP2pManager.setDnsSdResponseListeners(channel, serviceResponseListener, txtRecordListener);

        // Now that we are listening for services, begin to find them
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        // Tell the framework we want to scan for services. Prerequisite for discovering services
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

        // Initiates service discovery. Starts to scan for services we want to connect to
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

    public Map<String, DnsSdTxtRecord> getDnsSdTxtRecordMap() {
        return dnsSdTxtRecordMap;
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    /**
     * Removes a registered local service.
     */
    public void removeService() {
        if(serviceInfo != null) {
            Log.i(LOG_TAG, "Removing local service");
            wifiP2pManager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    serviceInfo = null;
                    Intent intent = new Intent(Action.SERVICE_REMOVED);
                    localBroadcastManager.sendBroadcast(intent);
                    Log.i(LOG_TAG, "Local service removed");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(LOG_TAG, "Failure removing local service: " + FailureReason.fromInteger(reason).toString());
                }
            });
        } else {
            Log.i(LOG_TAG, "No local service to remove");
        }
    }

    private void requestPeers() {
        // Initiates peer discovery
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Discovery initiation is successful. No services have actually been discovered yet
                // No data about peers can be collected here
                Log.i(LOG_TAG, "Initiate discovering peers successful");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure initiating discovering peers: " + FailureReason.fromInteger(reason).toString());
            }
        });
    }

  /**
   * Connects to a service
   * @param service The service to connect to
   */
    public void connectToService(DnsSdService service) {
        // Device info of peer to connect to
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

        // Starts a peer-to-peer connection with a device with the specified configuration
        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            // The ActionListener only notifies that initiation of connection has succeeded or failed

            @Override
            public void onSuccess() {
                // TODO fix log message
                Log.i(LOG_TAG, "Connected to service");
            }

            @Override
            public void onFailure(int reason) {
                // TODO fix log message
                Log.e(LOG_TAG, "Failure connecting to service: " + FailureReason.fromInteger(reason).toString());
            }
        });
    }

    /**
     * Creates a service that can be connected to without prompting. This is possible by creating an
     * access point and broadcasting the password for peers to use. Peers connect via normal wifi, not
     * wifi direct, but the effect is the same.
     */
    public void startAddingNoPromptService(ServiceData serviceData) {

        if (serviceInfo != null) {
            removeService();
        }
        isCreatingNoPrompt = true;
        noPromptServiceData = serviceData;

        wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(LOG_TAG, "Group created successfully");
                //Note that you will have to wait for WIFI_P2P_CONNECTION_CHANGED_INTENT for group info
            }

            @Override
            public void onFailure(int reason) {
                Log.i(LOG_TAG, "Group creation failed: " + FailureReason.fromInteger(reason));

            }
        });
    }

    /**
     * Connects to a no prompt service
     * @param service The service to connect to
     */
    public void connectToNoPromptService(DnsSdService service) {
        removeService();
        WifiConfiguration configuration = new WifiConfiguration();
        DnsSdTxtRecord txtRecord = dnsSdTxtRecordMap.get(service.getSrcDevice().deviceAddress);
        if(txtRecord == null) {
            Log.e(LOG_TAG, "No dnsSdTxtRecord found for the no prommpt service");
            return;
        }
        // Quotes around these are required
        configuration.SSID = "\"" + txtRecord.getRecord().get(Keys.NO_PROMPT_NETWORK_NAME) + "\"";
        configuration.preSharedKey = "\"" + txtRecord.getRecord().get(Keys.NO_PROMPT_NETWORK_PASS) + "\"";
        int netId = wifiManager.addNetwork(configuration);

        //disconnect form current network and connect to this one
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        Log.i(LOG_TAG, "Connected to no prompt network");
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
            // The list of discovered peers has changed
            // Available extras: EXTRA_P2P_DEVICE_LIST
            Log.i(LOG_TAG, "List of discovered peers changed");
            if (wifiP2pManager != null) {
                // Request the updated list of discovered peers from wifiP2PManager
                wifiP2pManager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        WifiDirectHandler.this.peers = peers;
                        Intent intent = new Intent(Action.PEERS_CHANGED);
                        intent.putExtra(PEERS, peers);
                        localBroadcastManager.sendBroadcast(intent);
                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // The state of Wi-Fi P2P connectivity has changed
            // Here is where you can request group info
            // Available extras: EXTRA_WIFI_P2P_INFO, EXTRA_NETWORK_INFO, EXTRA_WIFI_P2P_GROUP
            // I don't think we need anything from EXTRA_WIFI_P2P_INFO
            Log.i(LOG_TAG, "Wi-Fi P2P Connection Changed");

            WifiP2pInfo extraWifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            boolean groupFormed = extraWifiP2pInfo.groupFormed;
            boolean isGroupOwner = extraWifiP2pInfo.isGroupOwner;
            InetAddress groupOwnerAddress = extraWifiP2pInfo.groupOwnerAddress;
            Log.i(LOG_TAG, " ");
            Log.i(LOG_TAG, "EXTRA_WIFI_P2P_INFO:");
            Log.i(LOG_TAG, "- Group formed: " + groupFormed);
            Log.i(LOG_TAG, "- Is group owner: " + isGroupOwner);
            Log.i(LOG_TAG, "- Group owner address: " + groupOwnerAddress);

            WifiP2pGroup extraWifiP2PGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            Log.i(LOG_TAG, " ");
            Log.i(LOG_TAG, "EXTRA_WIFI_P2P_GROUP");
            Log.i(LOG_TAG, extraWifiP2PGroup.toString());
            Log.i(LOG_TAG, " ");

            if (wifiP2pManager != null && groupFormed) {
                // Requests peer-to-peer group information
                wifiP2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        if (group == null) {
                            Log.i(LOG_TAG, "No Wi-Fi P2P group found");
                        } else {
                            Log.i(LOG_TAG, "Group info available " + group.toString());
                            Log.i(LOG_TAG, "Group name: " + group.getNetworkName() + " - Pass: " + group.getPassphrase());
                        }

                        if (isCreatingNoPrompt) {
                            if (group == null) {
                                Log.e(LOG_TAG, "Adding no prompt service failed, group does not exist");
                                return;
                            }
                            isCreatingNoPrompt = false;

                            noPromptServiceData.getRecord().put(Keys.NO_PROMPT_NETWORK_NAME, group.getNetworkName());
                            noPromptServiceData.getRecord().put(Keys.NO_PROMPT_NETWORK_PASS, group.getPassphrase());

                            startAddingLocalService(noPromptServiceData);
                        }
                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Indicates whether Wi-Fi P2P is enabled
            // Determine if Wi-Fi P2P mode is enabled or not, alert the Activity
            // Available extras: EXTRA_WIFI_STATE
            // Sticky Intent
            Log.i(LOG_TAG, "Wi-Fi P2P State Changed:");
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wi-Fi Direct is enabled
                Log.i(LOG_TAG, "- Wi-Fi Direct is enabled");
            } else {
                // Wi-Fi Direct is not enabled
                Log.i(LOG_TAG, "- Wi-Fi Direct is not enabled");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Indicates this device's configuration details have changed
            // Sticky Intent
            Log.i(LOG_TAG, "This device changed");
        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            // Broadcast intent action indicating that peer discovery has either started or stopped
            // Available extras: EXTRA_DISCOVERY_STATE
            // Note that discovery will be stopped during a connection setup
            // If the application tries to re-initiate discovery during this time, it can fail
            Log.i(LOG_TAG, "** DISCOVERY STATE CHANGED **");
//            String discoveryState = intent.getParcelableExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE).toString();
//            Log.i(LOG_TAG, "- " + discoveryState);
        }
    }

  /**
   * Toggle wifi
   * @param wifiEnabled whether or not wifi should be enabled
   */
    public void setWifiEnabled(boolean wifiEnabled) {
        wifiManager.setWifiEnabled(wifiEnabled);
        if (wifiEnabled) {
            Log.i(LOG_TAG, "Wi-Fi enabled");
        } else {
            Log.i(LOG_TAG, "Wi-Fi disabled");
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
   * Actions that can be broadcast by the handler
   */
    public class Action {
        public static final String DNS_SD_TXT_RECORD_ADDED = "dnsSdTxtRecordAdded",
        DNS_SD_SERVICE_AVAILABLE = "dnsSdServiceAvailable",
        SERVICE_REMOVED = "serviceRemoved",
        PEERS_CHANGED = "peersChanged";
    }

    private class Keys {
        public static final String NO_PROMPT_NETWORK_NAME = "networkName",
        NO_PROMPT_NETWORK_PASS = "passphrase";
    }

    private class WifiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            onHandleIntent(intent);
        }
    }
}
