package edu.rit.se.wifibuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

// TODO: Add JavaDoc
public class WifiDirectHandler extends NonStopIntentService implements
        WifiP2pManager.ConnectionInfoListener,
        Handler.Callback{

    private static final String ANDROID_SERVICE_NAME = "Wi-Fi Buddy";
    public static final String TAG = "wfd_";
    private final IBinder binder = new WifiTesterBinder();

    public static final String SERVICE_MAP_KEY = "serviceMapKey";
    public static final String MESSAGE_KEY = "messageKey";
    private final String PEERS = "peers";

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;
    private List<ServiceDiscoveryTask> serviceDiscoveryTasks;
    private WifiP2pDeviceList peers;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver p2pBroadcastReceiver;
    private BroadcastReceiver wifiBroadcastReceiver;
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pServiceRequest serviceRequest;
    private Boolean isWifiP2pEnabled;
    private Handler handler = new Handler((Handler.Callback) this);
    private CommunicationManager communicationManager = null;
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    public static final int COMMUNICATION_DISCONNECTED = 0x400 + 3;
    public static final int SERVER_PORT = 4545;
    private final int SERVICE_DISCOVERY_TIMEOUT = 120000;

    private boolean continuouslyDiscovering = false;
    private boolean groupFormed = false;
    private boolean serviceDiscoveryRegistered = false;

    // Flag for creating a no prompt service
    private boolean isCreatingNoPrompt = false;
    private ServiceData noPromptServiceData;

    // Variables created in onCreate()
    private WifiP2pManager.Channel channel;
    private WifiP2pManager wifiP2pManager;
    private WifiManager wifiManager;

    private WifiP2pDevice thisDevice;
    private WifiP2pGroup wifiP2pGroup;

    /** Constructor **/
    public WifiDirectHandler() {
        super(ANDROID_SERVICE_NAME);
        dnsSdTxtRecordMap = new HashMap<>();
        dnsSdServiceMap = new HashMap<>();
        peers = new WifiP2pDeviceList();
    }

    /**
     * Registers the Wi-Fi manager, registers the app with the Wi-Fi P2P framework, registers the
     * P2P BroadcastReceiver, and registers a local BroadcastManager
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Creating WifiDirectHandler");

        // Registers the Wi-Fi Manager and the Wi-Fi BroadcastReceiver
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        registerWifiReceiver();

        if (wifiManager.isWifiEnabled()) {
            Log.i(TAG, "Wi-Fi enabled on load");
        } else {
            Log.i(TAG, "Wi-Fi disabled on load");
        }

        // Registers a local BroadcastManager that is used to broadcast Intents to Activities
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Log.i(TAG, "WifiDirectHandler created");
    }

    /**
     * Registers the application with the Wi-Fi P2P framework
     * Initializes the P2P manager and gets a P2P communication channel
     */
    public void registerP2p() {
        // Manages Wi-Fi P2P connectivity
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);

        // initialize() registers the app with the Wi-Fi P2P framework
        // Channel is used to communicate with the Wi-Fi P2P framework
        // Main Looper is the Looper for the main thread of the current process
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        Log.i(TAG, "Registered with Wi-Fi P2P framework");
    }

    /**
     * Unregisters the application with the Wi-Fi P2P framework
     */
    public void unregisterP2p() {
        if (wifiP2pManager != null) {
            wifiP2pManager = null;
            channel = null;
            Log.i(TAG, "Unregistered with Wi-Fi P2P framework");
        }
    }

    /**
     * Registers a WifiDirectBroadcastReceiver with an IntentFilter listening for P2P Actions
     */
    public void registerP2pReceiver() {
        p2pBroadcastReceiver = new WifiDirectBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();

        // Indicates a change in the list of available peers
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates a change in the Wi-Fi P2P status
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        registerReceiver(p2pBroadcastReceiver, intentFilter);
        Log.i(TAG, "P2P BroadcastReceiver registered");
    }

    /**
     * Unregisters the WifiDirectBroadcastReceiver and IntentFilter
     */
    public void unregisterP2pReceiver() {
        if (p2pBroadcastReceiver != null) {
            unregisterReceiver(p2pBroadcastReceiver);
            p2pBroadcastReceiver = null;
            Log.i(TAG, "P2P BroadcastReceiver unregistered");
        }
    }

    public void registerWifiReceiver() {
        wifiBroadcastReceiver = new WifiBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();

        // Indicates that Wi-Fi has been enabled, disabled, enabling, disabling, or unknown
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiBroadcastReceiver, intentFilter);
        Log.i(TAG, "Wi-Fi BroadcastReceiver registered");
    }

    public void unregisterWifiReceiver() {
        if (wifiBroadcastReceiver != null) {
            unregisterReceiver(wifiBroadcastReceiver);
            wifiBroadcastReceiver = null;
            Log.i(TAG, "Wi-Fi BroadcastReceiver unregistered");
        }
    }

    public void unregisterWifi() {
        if (wifiManager != null) {
            wifiManager = null;
            Log.i(TAG, "Wi-Fi manager unregistered");
        }
    }

    /**
     * The requested connection info is available
     * @param p2pInfo Wi-Fi P2P connection info
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Log.i(TAG, "Connection info available");
//        if(!p2pInfo.groupFormed) {
//            return;
//        }

        Thread handler;
        if (p2pInfo.isGroupOwner) {
            Log.i(TAG, "Connected as group owner");
            try {
                handler = new OwnerSocketHandler(this.getHandler());
                handler.start();
            } catch (IOException e) {
                Log.i(TAG, "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.i(TAG, "Connected as peer");
            handler = new ClientSocketHandler(this.getHandler(), p2pInfo.groupOwnerAddress);
            handler.start();
        }

        localBroadcastManager.sendBroadcast(new Intent(Action.SERVICE_CONNECTED));
        localBroadcastManager.sendBroadcast(new Intent(Action.DEVICE_CHANGED));
    }

    // TODO add JavaDoc
    public void addLocalService(String serviceName, HashMap<String, String> serviceRecord) {
        // Logs information about local service
        Log.i(TAG, "Adding local service: " + serviceName);

        // Service information
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                serviceName,
                ServiceType.PRESENCE_TCP.toString(),
                serviceRecord
        );

        // Only add a local service if clearLocalServices succeeds
        wifiP2pManager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Add the local service
                wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "Local service added");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(TAG, "Failure adding local service: " + FailureReason.fromInteger(reason).toString());
                        serviceInfo = null;
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failure clearing local services: " + FailureReason.fromInteger(reason).toString());
                serviceInfo = null;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeGroup();
        removePersistentGroups();
        stopServiceDiscovery();
        removeService();
        unregisterP2pReceiver();
        unregisterP2p();
        unregisterWifiReceiver();
        unregisterWifi();
        Log.i(TAG, "Wifi Handler service destroyed");
    }

    /**
     * Removes persistent/remembered groups
     *
     * Source: https://android.googlesource.com/platform/cts/+/jb-mr1-dev%5E1%5E2..jb-mr1-dev%5E1/
     * Author: Nick  Kralevich <nnk@google.com>
     *
     * WifiP2pManager.java has a method deletePersistentGroup(), but it is not accessible in the
     * SDK. According to Vinit Deshpande <vinitd@google.com>, it is a common Android paradigm to
     * expose certain APIs in the SDK and hide others. This allows Android to maintain stability and
     * security. As a workaround, this removePersistentGroups() method uses Java reflection to call
     * the hidden method. We can list all the methods in WifiP2pManager and invoke "deletePersistentGroup"
     * if it exists. This is used to remove all possible persistent/remembered groups. 
     */
    private void removePersistentGroups() {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Remove any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(wifiP2pManager, channel, netid, null);
                    }
                }
            }
            Log.i(TAG, "Persistent groups removed");
        } catch(Exception e) {
            Log.e(TAG, "Failure removing persistent groups: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes the current WifiP2pGroup in the WifiP2pChannel.
     */
    private void removeGroup() {
        if (wifiP2pGroup != null) {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    wifiP2pGroup = null;
                    Log.i(TAG, "Group removed");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failure removing group: " + FailureReason.fromInteger(reason).toString());
                }
            });
        }
    }

    private void registerServiceDiscoveryListeners() {
        // DnsSdTxtRecordListener
        // Interface for callback invocation when Bonjour TXT record is available for a service
        // Used to listen for incoming records and get peer device information
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                // Records of peer are available
                Log.i(TAG, "Peer DnsSDTxtRecord available");

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
                Log.i(TAG, "Local service found:");
                Log.i(TAG, deviceToString(srcDevice));
                dnsSdServiceMap.put(srcDevice.deviceAddress, new DnsSdService(instanceName, registrationType, srcDevice));
                Intent intent = new Intent(Action.DNS_SD_SERVICE_AVAILABLE);
                intent.putExtra(SERVICE_MAP_KEY, srcDevice.deviceAddress);
                localBroadcastManager.sendBroadcast(intent);
            }
        };

        wifiP2pManager.setDnsSdResponseListeners(channel, serviceResponseListener, txtRecordListener);
        Log.i(TAG, "Service discovery listeners registered");
    }

    private void addServiceDiscoveryRequest() {
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        // Tell the framework we want to scan for services. Prerequisite for discovering services
        wifiP2pManager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Service discovery request added");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failure adding service discovery request: " + FailureReason.fromInteger(reason).toString());
                serviceRequest = null;
            }
        });
    }

    /**
     * Initiates a service discovery. This has a 2 minute timeout. To continuously
     * discover services use continuouslyDiscoverServices
     */
    public void discoverServices(){
        // Initiates service discovery. Starts to scan for services we want to connect to
        wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failure initiating service discovery: " + FailureReason.fromInteger(reason).toString());
            }
        });
    }

    /**
     * Calls initial services discovery call and submits the first
     * Discover task. This will continue until stopDiscoveringServices is called
     */
    public void continuouslyDiscoverServices(){
        Log.i(TAG, "Continuously Discover services called");

        if (serviceDiscoveryRegistered == false) {
            Log.i(TAG, "Setting up service discovery");
            registerServiceDiscoveryListeners();
            addServiceDiscoveryRequest();
            serviceDiscoveryRegistered = true;
        }

        // TODO Change this to give some sort of status
        if (continuouslyDiscovering){
            Log.w(TAG, "Services are still discovering, do not need to make this call");
        } else {
            Log.i(TAG, "Calling discover and submitting first discover task");
            continuouslyDiscovering = true;
            // List to track discovery tasks in progress
            serviceDiscoveryTasks = new ArrayList<>();
            // Make discover call and first discover task submission
            discoverServices();
            submitServiceDiscoveryTask();
        }
    }

    public void stopServiceDiscovery() {
        dnsSdServiceMap = null;
        dnsSdTxtRecordMap = null;
        continuouslyDiscovering = false;
        // Cancel all discover tasks that may be in progress
        for (ServiceDiscoveryTask serviceDiscoveryTask : serviceDiscoveryTasks) {
            serviceDiscoveryTask.cancel();
        }
        serviceDiscoveryTasks = null;
        Log.i(TAG, "Service discovery stopped");
        clearServiceDiscoveryRequests();
    }

    /**
     * Submits a new task to initiate service discovery after the discovery
     * timeout period has expired
     */
    private void submitServiceDiscoveryTask(){
        Log.i(TAG, "Submitting service discovery task");
        // Discover times out after 2 minutes so we set the timer to that
        int timeToWait = SERVICE_DISCOVERY_TIMEOUT;
        ServiceDiscoveryTask serviceDiscoveryTask = new ServiceDiscoveryTask();
        Timer timer = new Timer();
        // Submit the service discovery task and add it to the list
        timer.schedule(serviceDiscoveryTask, timeToWait);
        serviceDiscoveryTasks.add(serviceDiscoveryTask);
    }

    /**
     * Timed task to initiate a new services discovery. Will recursively submit
     * a new task as long as continuouslyDiscovering is true
     */
    private class ServiceDiscoveryTask extends TimerTask {
        public void run() {
            discoverServices();
            // Submit the next task if a stop call hasn't been made
            if (continuouslyDiscovering) {
                submitServiceDiscoveryTask();
            }
            // Remove this task from the list since it's complete
            serviceDiscoveryTasks.remove(this);
        }
    }

    public Map<String, DnsSdService> getDnsSdServiceMap(){
        return dnsSdServiceMap;
    }

    public Map<String, DnsSdTxtRecord> getDnsSdTxtRecordMap() {
        return dnsSdTxtRecordMap;
    }

    /**
     * Uses wifiManager to determine if Wi-Fi is enabled
     * @return Whether Wi-Fi is enabled or not
     */
    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    /**
     * Removes a registered local service.
     */
    public void removeService() {
        if(serviceInfo != null) {
            Log.i(TAG, "Removing local service");
            wifiP2pManager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    serviceInfo = null;
                    Intent intent = new Intent(Action.SERVICE_REMOVED);
                    localBroadcastManager.sendBroadcast(intent);
                    Log.i(TAG, "Local service removed");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failure removing local service: " + FailureReason.fromInteger(reason).toString());
                }
            });
            serviceInfo = null;
        } else {
            Log.i(TAG, "No local service to remove");
        }
    }

    private void clearServiceDiscoveryRequests() {
        if (serviceRequest != null) {
            wifiP2pManager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    serviceRequest = null;
                    Log.i(TAG, "Service discovery requests cleared");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failure clearing service discovery requests: " + FailureReason.fromInteger(reason).toString());
                }
            });
        }
    }

    /**
     * Removes a service discovery request and initiates a connection to a service
     * @param service The service to connect to
     */
    public void initiateConnectToService(DnsSdService service) {
        // Device info of peer to connect to
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.getSrcDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if(serviceRequest != null) {
            wifiP2pManager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Service request removed");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failure removing service request: " + FailureReason.fromInteger(reason).toString());
                }
            });
        }

        // TODO: Should this go in the onSuccess() method above, in removeServiceRequest()?
        // Starts a peer-to-peer connection with a device with the specified configuration
        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            // The ActionListener only notifies that initiation of connection has succeeded or failed

            @Override
            public void onSuccess() {
//                wifiP2pManager.requestConnectionInfo(channel, WifiDirectHandler.this);
                Log.i(TAG, "Initiating connection to service");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failure initiating connection to service: " + FailureReason.fromInteger(reason).toString());
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
                Log.i(TAG, "Group created successfully");
                //Note that you will have to wait for WIFI_P2P_CONNECTION_CHANGED_INTENT for group info
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "Group creation failed: " + FailureReason.fromInteger(reason));

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
            Log.e(TAG, "No dnsSdTxtRecord found for the no prompt service");
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
        Log.i(TAG, "Connected to no prompt network");
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
            handlePeersChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // The state of Wi-Fi P2P connectivity has changed
            handleConnectionChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Indicates whether Wi-Fi P2P is enabled
            handleStateChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Indicates this device's configuration details have changed
            handleThisDeviceChanged(intent);
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            handleWifiStateChanged(intent);
        }
    }

    private void handleWifiStateChanged(Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            // Register app with Wi-Fi P2P framework, register WifiDirectBroadcastReceiver
            Log.i(TAG, "Wi-Fi enabled");
            registerP2p();
            registerP2pReceiver();
        } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
            // Remove local service, unregister app with Wi-Fi P2P framework, unregister P2pReceiver
            Log.i(TAG, "Wi-Fi disabled");
            clearServiceDiscoveryRequests();
            removeService();
            unregisterP2pReceiver();
            unregisterP2p();
        }
        localBroadcastManager.sendBroadcast(new Intent(Action.WIFI_STATE_CHANGED));
    }

    /**
     * The list of discovered peers has changed
     * Available extras: EXTRA_P2P_DEVICE_LIST
     * @param intent
     */
    private void handlePeersChanged(Intent intent) {
        Log.i(TAG, "List of discovered peers changed");
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
    }

    /**
     * The state of Wi-Fi P2P connectivity has changed
     * Here is where you can request group info
     * Available extras: EXTRA_WIFI_P2P_INFO, EXTRA_NETWORK_INFO, EXTRA_WIFI_P2P_GROUP
     * I don't think we need anything from EXTRA_NETWORK_INFO
     * @param intent
     */
    private void handleConnectionChanged(Intent intent) {
        Log.i(TAG, "Wi-Fi P2P Connection Changed");

        if(wifiP2pManager == null) {
            return;
        }

        NetworkInfo networkInfo = (NetworkInfo) intent
                .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        if(networkInfo.isConnected()) {
            Log.i(TAG, "Connected to p2p network. Requesting connection details");
            wifiP2pManager.requestConnectionInfo(channel, WifiDirectHandler.this);
        }

        // TODO: clean this up
//        // Extra information from EXTRA_WIFI_P2P_INFO
//        WifiP2pInfo extraWifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
//        groupFormed = extraWifiP2pInfo.groupFormed;
//        boolean isGroupOwnerP2pInfo = extraWifiP2pInfo.isGroupOwner;
//        InetAddress groupOwnerAddress = extraWifiP2pInfo.groupOwnerAddress;
//
//        // Extra information from EXTRA_WIFI_P2P_GROUP
//        WifiP2pGroup extraWifiP2PGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
//        Boolean isGroupOwnerP2pGroup = extraWifiP2PGroup.isGroupOwner();
//        String networkName = extraWifiP2PGroup.getNetworkName();
//        String passphrase = extraWifiP2PGroup.getPassphrase();
//        Collection<WifiP2pDevice> clients = extraWifiP2PGroup.getClientList();
//        String strClients = "";
//        for (WifiP2pDevice client : clients) {
//            strClients += deviceToString(client);
//        }
//        WifiP2pDevice owner = extraWifiP2PGroup.getOwner();
//
//        if (wifiP2pManager != null && groupFormed) {
//            // Logs extra information from EXTRA_WIFI_P2P_INFO
//            Log.i(TAG, "\nEXTRA_WIFI_P2P_INFO:");
//            Log.i(TAG, "- Group formed");
//            Log.i(TAG, "- Is group owner: " + isGroupOwnerP2pInfo);
//            Log.i(TAG, "- Group owner address: " + groupOwnerAddress);
//
//            // Logs extra information from EXTRA_WIFI_P2P_GROUP
//            Log.i(TAG, "\nEXTRA_WIFI_P2P_GROUP");
//            Log.i(TAG, "- Is group owner: " + isGroupOwnerP2pGroup);
//            Log.i(TAG, "- Network name: ");
//            Log.i(TAG, "    " + networkName);
//            Log.i(TAG, "- Passphrase: " + passphrase);
//            if (strClients.equals("")) {
//                Log.i(TAG, "- Clients: None");
//            } else {
//                Log.i(TAG, "- Clients:");
//                Log.i(TAG, strClients);
//            }
//            Log.i(TAG, strClients);
//            if (owner == null) {
//                Log.i(TAG, "- Owner: None");
//            } else {
//                Log.i(TAG, "- Owner:");
//                Log.i(TAG, deviceToString(owner));
//            }
//
            // Requests peer-to-peer group information
            wifiP2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    wifiP2pGroup = group;
                    Log.i(TAG, "Group info available");
                }
            });
//            Thread communicationThread;
//            if (isGroupOwnerP2pInfo) {
//                Log.i(TAG, "Connected as group owner");
//                try {
//                    communicationThread = new OwnerSocketHandler(this.getHandler());
//                    communicationThread.start();
//                } catch (IOException e) {
//                    Log.e(TAG, "Failed to create a server thread - " + e.getMessage());
//                    return;
//                }
//            } else {
//                Log.i(TAG, "Connected as peer");
//                communicationThread = new ClientSocketHandler(this.getHandler(), groupOwnerAddress);
//                communicationThread.start();
//            }
//
//            Intent connectionIntent = new Intent(Action.SERVICE_CONNECTED);
//            localBroadcastManager.sendBroadcast(connectionIntent);
//        }
    }

    /**
     * Indicates whether Wi-Fi P2P is enabled
     * Determine if Wi-Fi P2P mode is enabled or not, alert the Activity
     * Available extras: EXTRA_WIFI_STATE
     * Sticky Intent
     * @param intent
     */
    private void handleStateChanged(Intent intent) {
        Log.i(TAG, "Wi-Fi P2P State Changed:");
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            // Wi-Fi Direct is enabled
            isWifiP2pEnabled = true;
            Log.i(TAG, "- Wi-Fi Direct is enabled");
        } else {
            // Wi-Fi Direct is not enabled
            isWifiP2pEnabled = false;
            Log.i(TAG, "- Wi-Fi Direct is not enabled");
        }
    }

    /**
     * Indicates this device's configuration details have changed
     * Sticky Intent
     * @param intent
     */
    private void handleThisDeviceChanged(Intent intent) {
        Log.i(TAG, "This device changed");

        // Extra information from EXTRA_WIFI_P2P_DEVICE
        thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

        // Logs extra information from EXTRA_WIFI_P2P_DEVICE
        Log.i(TAG, deviceToString(thisDevice));

        localBroadcastManager.sendBroadcast(new Intent(Action.DEVICE_CHANGED));
    }

    /**
     * Toggle wifi
     * @param wifiEnabled whether or not wifi should be enabled
     */
    public void setWifiEnabled(boolean wifiEnabled) {
        wifiManager.setWifiEnabled(wifiEnabled);
    }

    public Handler getHandler() {
        return handler;
    }

    // TODO: Add JavaDoc
    @Override
    public boolean handleMessage(Message msg) {
        Log.i(TAG, "handleMessage() called");
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String receivedMessage = new String(readBuf, 0, msg.arg1);
                Log.i(TAG, "Received message: " + receivedMessage);
                Intent messageReceivedIntent = new Intent(Action.MESSAGE_RECEIVED);
                messageReceivedIntent.putExtra(MESSAGE_KEY, readBuf);
                localBroadcastManager.sendBroadcast(messageReceivedIntent);
                break;
            case MY_HANDLE:
                Object messageObject = msg.obj;
                communicationManager = (CommunicationManager) messageObject;
                break;
            case COMMUNICATION_DISCONNECTED:
                Log.i(TAG, "Handling communication disconnect");
                // TODO: handle disconnect
                break;
        }
        return true;
    }

    public CommunicationManager getCommunicationManager() {
        return communicationManager;
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
     * Actions that can be broadcast or received by the handler
     */
    public class Action {
        public static final String DNS_SD_TXT_RECORD_ADDED = "dnsSdTxtRecordAdded",
                DNS_SD_SERVICE_AVAILABLE = "dnsSdServiceAvailable",
                SERVICE_REMOVED = "serviceRemoved",
                PEERS_CHANGED = "peersChanged",
                SERVICE_CONNECTED = "serviceConnected",
                DEVICE_CHANGED = "deviceChanged",
                MESSAGE_RECEIVED = "messageReceived",
                WIFI_STATE_CHANGED = "wifiStateChanged";
    }

    private class Keys {
        public static final String NO_PROMPT_NETWORK_NAME = "networkName",
                NO_PROMPT_NETWORK_PASS = "passphrase";
    }

    // TODO: Add JavaDoc
    private class WifiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            onHandleIntent(intent);
        }
    }

    // TODO: Add JavaDoc
    private class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            onHandleIntent(intent);
        }
    }

    /**
     * Takes a WifiP2pDevice and returns a String of readable device information
     * @param device
     * @return
     */
    public String deviceToString(WifiP2pDevice device) {
        String strDevice = "";
        strDevice += "Device name: " + device.deviceName;
        strDevice += "\nDevice address: " + device.deviceAddress;
        strDevice += "\nIs group owner: " + device.isGroupOwner();
        strDevice += "\nStatus: " + deviceStatusToString(device.status) + "\n";
        return strDevice;
    }

    /**
     * Translates a device status code to a readable String status
     * @param status
     * @return A readable String device status
     */
    public String deviceStatusToString(int status) {
        if (status == WifiP2pDevice.AVAILABLE) {
            return "Available";
        } else if (status == WifiP2pDevice.INVITED) {
            return "Invited";
        } else if (status == WifiP2pDevice.CONNECTED) {
            return "Connected";
        } else if (status == WifiP2pDevice.FAILED) {
            return "Failed";
        } else if (status == WifiP2pDevice.UNAVAILABLE) {
            return "Unavailable";
        } else {
            return "Unknown";
        }
    }

    public String getThisDeviceInfo() {
        if (thisDevice == null) {
            return "No Device Info";
        } else {
            if (thisDevice.deviceName.equals("")) {
                thisDevice.deviceName = "Android Device";
            }
            return deviceToString(thisDevice);
        }
    }

    public WifiP2pDevice getThisDevice() {
        return thisDevice;
    }
}
