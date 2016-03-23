package edu.rit.se.crashavoidance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;

public class MainActivity extends AppCompatActivity implements
        ServicesList.DeviceClickListener,
        WifiP2pManager.ConnectionInfoListener,
        WiFiChatFragment.MessageTarget,
        Handler.Callback,
        OnLocationUpdatedListener {

    public static final String SERVICE_NAME = "_crashavoidance";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    static final int SERVER_PORT = 4545;
    private IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private TextView statusTextView;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private BroadcastReceiver receiver = null;
    private WiFiChatFragment chatFragment;
    private ServicesList servicesList;
    private Handler handler = new Handler(this);
    private LocationGooglePlayServicesProvider provider;
    // Tells us whether or not we are currently connected to a group
    private boolean inGroup = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);

        statusTextView = (TextView) findViewById(R.id.status_text);

        findViewById(R.id.off).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableConnections();
            }
        });

        findViewById(R.id.on).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableConnections();
            }
        });

        servicesList = new ServicesList();
        getFragmentManager().beginTransaction()
                .add(R.id.main_container, servicesList, "services").commit();

//        registerAndFindServices();
//        startLocation();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void enableConnections() {
        registerAndFindServices();
    }

    private void disableConnections() {
//        statusTextView.setText("");
        wifiP2pManager.clearLocalServices(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Local services cleared");
            }

            @Override
            public void onFailure(int reason) {
                appendStatus("Error clearing local services");
            }
        });

        wifiP2pManager.clearServiceRequests(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Cleared service requests");
            }

            @Override
            public void onFailure(int reason) {
                appendStatus("Error clearing service requests");
            }
        });

        if (inGroup) {
            wifiP2pManager.removeGroup(channel, new ActionListener() {

                @Override
                public void onSuccess() {
                    onDisconnect();
                }

                @Override
                public void onFailure(int reason) {
                    appendStatus("Error disconnecting from wifi group");
                }
            });
        } else {
            Log.d(SERVICE_NAME, "Not currently connected to a group, do not need to reset connection");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this);
        registerReceiver(receiver, intentFilter);
        discoverService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        disableConnections();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.init_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_disconnect:
                onClickMenuDisconnect(item);
                return true;
            case R.id.action_toggle_wifi:
                onClickMenuToggleWifi(item);
                return true;
            case R.id.action_view_logs:
                onClickMenuViewLogs(item);
                return true;
            case R.id.action_exit:
                onClickMenuExit(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickMenuDisconnect(MenuItem item) {
        displayToast("Disconnect tapped");
    }

    public void onClickMenuToggleWifi(MenuItem item) {
        displayToast("Toggle Wi-Fi tapped");
    }

    public void onClickMenuViewLogs(MenuItem item) {
        displayToast("View Logs tapped");
    }

    public void onClickMenuExit(MenuItem item) {
        finish();
    }

    private void registerAndFindServices() {
        Map<String, String> record = new HashMap<String, String>();
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_NAME, "_presence._tcp", record);
        wifiP2pManager.addLocalService(channel, service, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });

        discoverService();
    }

    private void discoverService() {
        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        wifiP2pManager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?

                        if (instanceName.equalsIgnoreCase(SERVICE_NAME)) {
                            appendStatus("Service available");
                            // update the UI and add the item the discovered
                            // device.
                            ServicesList fragment = (ServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                ServicesList.WiFiDevicesAdapter adapter = ((ServicesList.WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                adapter.addUnique(service);
                                connectP2p(service);
//                                adapter.add(service);
//                                adapter.notifyDataSetChanged();
                                Log.d(SERVICE_NAME, "onBonjourServiceAvailable "
                                        + instanceName);
                            }
                        }

                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(SERVICE_NAME,
                                device.deviceName + " is "
                                        + record.get("available"));
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wifiP2pManager.addServiceRequest(channel, serviceRequest,
                new ActionListener() {

                    @Override
                    public void onSuccess() {
                        appendStatus("Added service discovery request");
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        switch (errorCode) {
                            case WifiP2pManager.P2P_UNSUPPORTED:
                                appendStatus("Failed adding service, P2P unsupported on this device");
                                break;
                            case WifiP2pManager.BUSY:
                                appendStatus("Failed adding service, device is busy");
                                break;
                            default:
                                appendStatus("Failed adding service discovery request");
                        }
                    }
                });
        wifiP2pManager.discoverServices(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");

            }
        });
    }

    @Override
    public void connectP2p(WiFiP2pService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null)
            wifiP2pManager.removeServiceRequest(channel, serviceRequest,
                    new ActionListener() {

                        @Override
                        public void onSuccess() {
                            Log.d(SERVICE_NAME, "Successfully removed service request");
                        }

                        @Override
                        public void onFailure(int arg0) {
                            Log.d(SERVICE_NAME, "Failed to remove service request");
                        }
                    });

        wifiP2pManager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Connecting to service");
            }

            @Override
            public void onFailure(int errorCode) {
                String failureReason = "";
                if (errorCode == WifiP2pManager.BUSY) {
                    failureReason = "Busy";
                } else if (errorCode == WifiP2pManager.ERROR) {
                    failureReason = "Error";
                } else if (errorCode == WifiP2pManager.P2P_UNSUPPORTED) {
                    failureReason = "P2pUnsupported";
                }
                appendStatus("Failed connecting to service wth errorCode of " + failureReason);
            }

        });
}

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        if (p2pInfo.groupFormed && p2pInfo.isGroupOwner) {
            inGroup = true;
            Log.d(SERVICE_NAME, "Connected as group owner");
            statusTextView.setBackgroundColor(Color.BLUE);
            try {
                handler = new GroupOwnerSocketHandler(
                        ((WiFiChatFragment.MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(SERVICE_NAME,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else if (p2pInfo.groupFormed) {
            inGroup = true;
            Log.d(SERVICE_NAME, "Connected as peer");
            statusTextView.setBackgroundColor(Color.GREEN);
            handler = new ClientSocketHandler(
                    ((WiFiChatFragment.MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        } else {
            inGroup = false;
            Log.d(SERVICE_NAME, "Group connection was unsuccessful");
            statusTextView.setBackgroundColor(Color.RED);
        }
//        chatFragment = new WiFiChatFragment();
//        getFragmentManager().beginTransaction()
//                .replace(R.id.main_container, chatFragment).commit(); TODO: Re-enable after demo 2
    }

    private void appendStatus(String status) {
        statusTextView.append("\n" + status);
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(SERVICE_NAME, readMessage);
//                (chatFragment).pushMessage("Buddy: " + readMessage); TODO: Removed for demo 2
                break;

            case MY_HANDLE:
                Object obj = msg.obj;
//                (chatFragment).setChatManager((ChatManager) obj);

        }
        return true;
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (chatFragment != null) {
//            chatFragment.onLocationUpdated(location); TODO: Removed for demo 2
        }
    }

    public void onDisconnect() {
        inGroup = false;
//        statusTextView.setText("");
        appendStatus("Disconnected from wifi group");
        statusTextView.setBackgroundColor(Color.WHITE);
//        getFragmentManager().beginTransaction()
//                .remove(chatFragment)
//                .commit(); TODO: disabled for demo 2
        chatFragment = null;
        servicesList.listAdapter.clear();
//        registerAndFindServices();
    }

    private void startLocation() {
        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);

        SmartLocation smartLocation = new SmartLocation.Builder(this).logging(true).build();
        LocationParams params = new LocationParams.Builder()
                .setAccuracy(LocationAccuracy.HIGH)
                .setInterval(1000)
                .build();

        smartLocation.location(provider).config(params).start(this);
        appendStatus("Location started");
    }

    private void stopLocation() {
        SmartLocation.with(this).location().stop();
        appendStatus("Location stopped");
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://edu.rit.se.crashavoidance/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://edu.rit.se.crashavoidance/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    public void displayToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
