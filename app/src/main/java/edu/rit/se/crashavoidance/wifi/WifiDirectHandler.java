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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import edu.rit.se.crashavoidance.views.ChatFragment;
import edu.rit.se.crashavoidance.views.ChatFragment.MessageTarget;

/**
 * TODO add comment
 */
public class WifiDirectHandler extends NonStopIntentService implements
        WifiP2pManager.ConnectionInfoListener,
        MessageTarget,
        Handler.Callback{

    private static final String androidServiceName = "Wi-Fi Direct Handler";
    public static final String LOG_TAG = "wifiDirectHandler";
    private final IBinder binder = new WifiTesterBinder();

    public static final String SERVICE_MAP_KEY = "serviceMapKey";
    private final String PEERS = "peers";

    private Map<String, DnsSdTxtRecord> dnsSdTxtRecordMap;
    private Map<String, DnsSdService> dnsSdServiceMap;
    private List<DiscoverTask> discoverTasks;
    private WifiP2pDeviceList peers;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver receiver;
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pServiceRequest serviceRequest;
    private ChatFragment chatFragment;
    private Boolean isWifiP2pEnabled;
    private Handler handler = new Handler((Handler.Callback) this);
    private static final int MESSAGE_READ = 0x400 + 1;
    private static final int MY_HANDLE = 0x400 + 2;

    private boolean continueDiscovering = false;

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

    private WifiP2pDevice thisDevice;

    /**
     * Registers the app with the Wi-Fi P2P framework and registers a WifiDirectBroadcastReceiver
     * with an IntentFilter that listens for Wi-Fi P2P Actions
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "Creating WifiDirectHandler");

        // Manages Wi-Fi connectivity
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            Log.i(LOG_TAG, "Wi-Fi enabled on load");
            registerP2p();
            registerP2pReceiver();
        } else {
            Log.i(LOG_TAG, "Wi-Fi disabled on load");
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Log.i(LOG_TAG, "WifiDirectHandler created");
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
        Log.i(LOG_TAG, "Registered with Wi-Fi P2P framework");
    }

    /**
     * Unregisters the application with the Wi-Fi P2P framework
     */
    public void unregisterP2p() {
        wifiP2pManager = null;
        channel = null;
        Log.i(LOG_TAG, "Unregistered with Wi-Fi P2P framework");
    }

    /**
     * Registers a WifiDirectBroadcastReceiver with an IntentFilter listening for P2P Actions
     */
    public void registerP2pReceiver() {
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
        Log.i(LOG_TAG, "P2P BroadcastReceiver registered");
    }

    /**
     * Unregisters the WifiDirectBroadcastReceiver and IntentFilter
     */
    public void unregisterP2pReceiver() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        filter = null;
        Log.i(LOG_TAG, "P2P BroadcastReceiver unregistered");
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        Log.i(LOG_TAG, "Connection info available");
    }

    // TODO add JavaDoc
    public void startAddingLocalService(ServiceData serviceData) {
        Map<String, String> records = new HashMap<>(serviceData.getRecord());
        records.put("listenport", Integer.toString(serviceData.getPort()));
        records.put("available", "visible");

        // Logs information about local service
        Log.i(LOG_TAG, "Adding local service:");
        Log.i(LOG_TAG, serviceData.toString());

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeGroup();
        removeService();
        unregisterP2pReceiver();
        unregisterP2p();
        Log.i(LOG_TAG, "Wifi Handler service destroyed");
    }

    private void removeGroup() {
        wifiP2pManager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(LOG_TAG, "P2P negotiation canceled");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure canceling P2P negotiation: " + FailureReason.fromInteger(reason).toString());
            }
        });
        if (thisDevice.status == WifiP2pDevice.CONNECTED) {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(LOG_TAG, "Group removed");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(LOG_TAG, "Failure removing group: " + FailureReason.fromInteger(reason).toString());
                }
            });
        }
    }

    /**
   * Starts discovering services. First registers DnsSdTxtRecordListener and a
   * DnsSdServiceResponseListener. Then adds a service request and begins to discover services. The
   * callbacks within the registered listeners are called when services are found.
   */
    public void setupServiceDiscovery() {
        // DnsSdTxtRecordListener
        // Interface for callback invocation when Bonjour TXT record is available for a service
        // Used to listen for incoming records and get peer device information
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                // Records of peer are available
                Log.i(LOG_TAG, "Peer DnsSDTxtRecord available");

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
                Log.i(LOG_TAG, "Local service found:");
                Log.i(LOG_TAG, deviceToString(srcDevice));
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
    }

    /**
     * Initiates a service discovery. This has a 2 minute timeout. To continuously
     * discover services use continuouslyDiscoverServices
     */
    public void discoverServices(){
        Log.i(LOG_TAG, "Discover services called");
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

    /**
     * Calls initial services discovery call and submits the first
     * Discover task. This will continue until stopDiscoveringServices is called
     */
    public void continuouslyDiscoverServices(){
        // TODO Change this to give some sort of status
        Log.i(LOG_TAG, "Continuously Discover services called");
        if (continueDiscovering){
            Log.w(LOG_TAG, "Services are still discovering, do not need to make this call");
        } else {
            Log.i(LOG_TAG, "Calling discover and submitting first discover task");
            continueDiscovering = true;
            // List to track discovery tasks in progress
            discoverTasks = new ArrayList<>();
            // Make discover call and first discover task submission
            discoverServices();
            submitDiscoverTask();
        }
    }

    /**
     * Submits a new task to initiate service discovery after the discovery
     * timeout period has expired
     */
    private void submitDiscoverTask(){
        Log.i(LOG_TAG, "Submitting discover task");
        // Discover times out after 2 minutes so we set the timer to that
        int timeToWait = 120000;
        DiscoverTask task = new DiscoverTask();
        Timer timer = new Timer();
        // Submit the task and add it to the List
        timer.schedule(task, timeToWait);
        discoverTasks.add(task);
    }

    /**
     * Timed task to initiate a new services discovery. Will recursively submit
     * a new task as long as continueDiscovering is true
     */
    private class DiscoverTask extends TimerTask {
        public void run() {
            discoverServices();
            // Submit the next task if a stop call hasn't been made
            if (continueDiscovering){
                submitDiscoverTask();
            }
            // remove this task from the list since it's complete
            discoverTasks.remove(this);
        }
    }

    /**
     * Stop discovering services if continuous discovery was called
     */
    public void stopDiscoveringServices(){
        Log.i(LOG_TAG, "Service discovery called");
        if (continueDiscovering) {
            Log.i(LOG_TAG, "Service discovery being stopped");
            continueDiscovering = false;
            // Cancel all discover tasks that may be in progress
            for (DiscoverTask task : discoverTasks) {
                task.cancel();
            }
        // We don't want to do anything if we're not discovering services
        } else {
            Log.w(LOG_TAG, "Service discovery was not running, no action will be taken");
        }
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
   * Initiates a connection to a service
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
                wifiP2pManager.requestConnectionInfo(channel, WifiDirectHandler.this);
                Log.i(LOG_TAG, "Initiating connection to service");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failure initiating connection to service: " + FailureReason.fromInteger(reason).toString());
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
            Log.e(LOG_TAG, "No dnsSdTxtRecord found for the no prompt service");
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
            // I don't think we need anything from EXTRA_NETWORK_INFO

            Log.i(LOG_TAG, "Wi-Fi P2P Connection Changed");

            // Extra information from EXTRA_WIFI_P2P_INFO
            WifiP2pInfo extraWifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            boolean groupFormed = extraWifiP2pInfo.groupFormed;
            boolean isGroupOwnerP2pInfo = extraWifiP2pInfo.isGroupOwner;
            InetAddress groupOwnerAddress = extraWifiP2pInfo.groupOwnerAddress;

            // Extra information from EXTRA_WIFI_P2P_GROUP
            WifiP2pGroup extraWifiP2PGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            Boolean isGroupOwnerP2pGroup = extraWifiP2PGroup.isGroupOwner();
            String networkName = extraWifiP2PGroup.getNetworkName();
            String passphrase = extraWifiP2PGroup.getPassphrase();
            Collection<WifiP2pDevice> clients = extraWifiP2PGroup.getClientList();
            String strClients = "";
            for (WifiP2pDevice client : clients) {
                strClients += deviceToString(client);
            }
            WifiP2pDevice owner = extraWifiP2PGroup.getOwner();

            if (wifiP2pManager != null && groupFormed) {
                // Logs extra information from EXTRA_WIFI_P2P_INFO
                Log.i(LOG_TAG, "\nEXTRA_WIFI_P2P_INFO:");
                Log.i(LOG_TAG, "- Group formed");
                Log.i(LOG_TAG, "- Is group owner: " + isGroupOwnerP2pInfo);
                Log.i(LOG_TAG, "- Group owner address: " + groupOwnerAddress);

                // Logs extra information from EXTRA_WIFI_P2P_GROUP
                Log.i(LOG_TAG, "\nEXTRA_WIFI_P2P_GROUP");
                Log.i(LOG_TAG, "- Is group owner: " + isGroupOwnerP2pGroup);
                Log.i(LOG_TAG, "- Network name: ");
                Log.i(LOG_TAG, "    " + networkName);
                Log.i(LOG_TAG, "- Passphrase: " + passphrase);
                if (strClients.equals("")) {
                    Log.i(LOG_TAG, "- Clients: None");
                } else {
                    Log.i(LOG_TAG, "- Clients:");
                    Log.i(LOG_TAG, strClients);
                }
                Log.i(LOG_TAG, strClients);
                if (owner == null) {
                    Log.i(LOG_TAG, "- Owner: None");
                } else {
                    Log.i(LOG_TAG, "- Owner:");
                    Log.i(LOG_TAG, deviceToString(owner));
                }

                // Requests peer-to-peer group information
                wifiP2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        Log.i(LOG_TAG, "Requesting group info");

                        if (isCreatingNoPrompt) {
                            if (group == null) {
                                Log.e(LOG_TAG, "- Adding no prompt service failed, group does not exist");
                                return;
                            }
                            isCreatingNoPrompt = false;

                            noPromptServiceData.getRecord().put(Keys.NO_PROMPT_NETWORK_NAME, group.getNetworkName());
                            noPromptServiceData.getRecord().put(Keys.NO_PROMPT_NETWORK_PASS, group.getPassphrase());

                            startAddingLocalService(noPromptServiceData);
                        }
                    }
                });

                Thread handler;
                if (isGroupOwnerP2pInfo) {
                    Log.i(LOG_TAG, "Connected as group owner");
                    try {
                        handler = new OwnerSocketHandler(this.getHandler());
                        handler.start();
                    } catch (IOException e) {
                        Log.i(LOG_TAG, "Failed to create a server thread - " + e.getMessage());
                        return;
                    }
                } else {
                    Log.i(LOG_TAG, "Connected as peer");
                    handler = new ClientSocketHandler(this.getHandler(), groupOwnerAddress);
                    handler.start();
                }

                Intent connectionIntent = new Intent(Action.SERVICE_CONNECTED);
                localBroadcastManager.sendBroadcast(connectionIntent);
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
                isWifiP2pEnabled = true;
                Log.i(LOG_TAG, "- Wi-Fi Direct is enabled");
            } else {
                // Wi-Fi Direct is not enabled
                isWifiP2pEnabled = false;
                Log.i(LOG_TAG, "- Wi-Fi Direct is not enabled");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Indicates this device's configuration details have changed
            // Sticky Intent

            Log.i(LOG_TAG, "This device changed");

            // Extra information from EXTRA_WIFI_P2P_DEVICE
            thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            Intent deviceChangedIntent = new Intent(Action.DEVICE_CHANGED);
            localBroadcastManager.sendBroadcast(deviceChangedIntent);

            // Logs extra information from EXTRA_WIFI_P2P_DEVICE
            Log.i(LOG_TAG, deviceToString(thisDevice));
        }
    }

  /**
   * Toggle wifi
   * @param wifiEnabled whether or not wifi should be enabled
   */
    public void setWifiEnabled(boolean wifiEnabled) {
        wifiManager.setWifiEnabled(wifiEnabled);
        if (wifiEnabled) {
            // Register app with Wi-Fi P2P framework, register WifiDirectBroadcastReceiver
            Log.i(LOG_TAG, "Wi-Fi enabled");
            registerP2p();
            registerP2pReceiver();
        } else {
            // Remove local service, unregister app with Wi-Fi P2P framework,
            //   unregister WifiDirectBroadcastReceiver
            Log.i(LOG_TAG, "Wi-Fi disabled");
            removeService();
            unregisterP2pReceiver();
            unregisterP2p();
        }
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.e(LOG_TAG, "handleMessage() called");
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d("wifiDirectTester", readMessage);
                chatFragment.pushMessage("Buddy: " + readMessage);
                break;
            case MY_HANDLE:
                Object obj = msg.obj;
                chatFragment.setChatManager((ChatManager) obj);
        }
        return true;
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
        DEVICE_CHANGED = "deviceChanged";
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

    public void setChatFragment(ChatFragment theFragment){
        chatFragment = theFragment;
    }

    public String deviceToString(WifiP2pDevice device) {
        String strDevice = "";
        strDevice += "Device name: " + device.deviceName;
        strDevice += "\nDevice address: " + device.deviceAddress;
        strDevice += "\nIs group owner: " + device.isGroupOwner();
        strDevice += "\nStatus: " + deviceStatusToString(device.status) + "\n";
        return strDevice;
    }

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
            return deviceToString(thisDevice);
        }
    }

    public WifiP2pDevice getThisDevice() {
        return thisDevice;
    }
}
