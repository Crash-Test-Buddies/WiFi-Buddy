package edu.rit.se.crashavoidance.views;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Bundle;
<<<<<<< HEAD
import android.os.IBinder;
=======
import android.support.v4.app.FragmentManager;
>>>>>>> wifiTester
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

public class initActivity extends AppCompatActivity {

    // Buttons
    private Button toggleWifiButton;
    private Button receiverRegistrationButton;
    private Button wifiDirectRegistrationButton;
    private Button serviceRegistrationButton;
    private Button discoverServicesButton;

    // Services
    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    static final int SERVER_PORT = 4545;

    // Fragment Manager
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        // Initialize Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.initToolbar);
        setSupportActionBar(toolbar);

        // Fragmnet Manager
        fragmentManager = getSupportFragmentManager();
        
        // Initialize Buttons
        toggleWifiButton = (Button) findViewById(R.id.toggleWifiButton);
        wifiDirectRegistrationButton = (Button) findViewById(R.id.wifiDirectRegistrationButton);
        receiverRegistrationButton = (Button) findViewById(R.id.receiverRegistrationButton);
        serviceRegistrationButton = (Button) findViewById(R.id.serviceRegistrationButton);
        discoverServicesButton = (Button) findViewById(R.id.discoverServicesButton);

        // Set Toggle Wi-Fi Button based on Wi-Fi state
        if(wifiDirectHandler.isWifiEnabled()){
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
        } else {
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(wifiDirectHandlerBound) {
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, WifiDirectHandler.class);
        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Adds Main Menu to the ActionBar
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_logs:
                // View Logs MenuItem tapped
                onClickMenuViewLogs(item);
                return true;
            case R.id.action_exit:
                // Exit MenuItem tapped
                onClickMenuExit(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickButtonToggleWifi(View view) {
        String action = toggleWifiButton.getText().toString();
        if (action.equals(getString(R.string.action_enable_wifi))) {
            // Enable Wi-Fi
            enableWifi();
        } else if (action.equals(getString(R.string.action_disable_wifi))) {
            // Disable Wi-Fi and other dependent services (Service, Receiver, Wi-Fi Direct Registration)
            unregisterService();
            unregisterReceiver();
            unregisterWifiDirect();
            disableWifi();
        }
    }

    public void onClickButtonWifiDirectRegistration(View view) {
        String action = wifiDirectRegistrationButton.getText().toString();
        if (action.equals(getString(R.string.action_register_wifi_direct))) {
            // Register Wi-Fi Direct
            registerWifiDirect();
        } else if (action.equals(getString(R.string.action_unregister_wifi_direct))) {
            // Unregister Wi-Fi Direct, Unregister BroadcastReceiver, and Unregister Services
            unregisterService();
            unregisterReceiver();
            unregisterWifiDirect();
        }
    }

    public void onClickButtonReceiverRegistration(View view) {
        String action = receiverRegistrationButton.getText().toString();
        if (action.equals(getString(R.string.action_register_receiver))) {
            // Register Wi-Fi Direct BroadcastReceiver
            //TODO: What does this do - registerReceiver();
        } else if (action.equals(getString(R.string.action_unregister_receiver))) {
            // Unregister Wi-Fi Direct BroadcastReceiver
            unregisterReceiver();
        }
    }

    public void onClickButtonServiceRegistration(View view) {
        String action = serviceRegistrationButton.getText().toString();
        if (action.equals(getString(R.string.action_register_service))) {
            // Register Local Service
            registerService();
        } else if (action.equals(getString(R.string.action_unregister_service))) {
            // Unregister Local Service
            unregisterService();
        }
    }

    public void onClickButtonDiscoverServices(View view) {
        discoverServices();
    }

    public void onClickMenuViewLogs(MenuItem item) {
        // Open the View Logs Dialog Fragment
        Log.i(getString(R.string.log_tag), getString(R.string.status_viewing_logs));
        LogsDialogFragment logsDialogFragment = new LogsDialogFragment();
        logsDialogFragment.show(getFragmentManager(), "dialog");
    }

    public void onClickMenuExit(MenuItem item) {
        // Terminate the app
        finish();
    }

    private void enableWifi() {
        if (!wifiDirectHandler.isWifiEnabled()) {
            // Enable Wi-Fi
            wifiDirectHandler.setWifiEnabled(true);
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
            displayToast(getString(R.string.status_wifi_enabled));
        } else {
            // Failed to enable Wi-Fi - Wi-Fi is already enabled
            displayToast(getString(R.string.warning_wifi_already_enabled));
        }
    }

    private void disableWifi() {
        if (wifiDirectHandler.isWifiEnabled()) {
            // Disable Wi-Fi
            wifiDirectHandler.setWifiEnabled(false);
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
            displayToast(getString(R.string.status_wifi_disabled));
        } else {
            // Failed to disable Wi-Fi - Wi-Fi is already disabled
            displayToast(getString(R.string.warning_wifi_already_disabled));
        }
    }

    private void registerWifiDirect() {
        if (wifiDirectHandler.isWifiEnabled()) {
            // Wi-Fi is enabled, continue registration
//            wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
//            wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
//            wifiDirectHandler.setManager(wifiP2pManager);
//            wifiDirectHandler.setChannel(wifiP2pChannel);
            wifiDirectRegistrationButton.setText(getString(R.string.action_unregister_wifi_direct));
            displayToast(getString(R.string.status_wifi_direct_initialized));
        } else {
            // Wi-Fi isn't enabled, stop registration
            displayToast(getString(R.string.warning_wifi_direct_wifi_disabled));
        }
    }

    private void unregisterWifiDirect() {
//        if (wifiP2pManager != null || wifiP2pChannel != null) {
//            // Unregister Wi-Fi Direct
//            wifiP2pManager = null;
//            wifiP2pChannel = null;

            wifiDirectRegistrationButton.setText(getString(R.string.action_register_wifi_direct));
            displayToast(getString(R.string.status_wifi_direct_unregistered));
//        }
    }

//    private void registerReceiver() {
//        if (wifiDirectHandler.isWifiEnabled()) {
//            // Wi-Fi is enabled, continue registration
//            if (wifiP2pManager != null && wifiP2pChannel != null) {
//                // Wi-Fi Direct is registered, start Broadcast Receiver Registration
//                wifiP2pReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, this);
//                wifiP2pReceiver.registerReceiver();
//                receiverRegistrationButton.setText(getString(R.string.action_unregister_receiver));
//                displayToast(getString(R.string.status_receiver_registered));
//            } else {
//                // Wi-Fi Direct hasn't been registered
//                displayToast(getString(R.string.warning_receiver_wifi_direct));
//            }
//        } else {
//            // Wi-Fi hasn't been enabled
//            displayToast(getString(R.string.warning_receiver_wifi));
//        }
//    }

//    private void unregisterReceiver() {
//        if (wifiP2pReceiver != null) {
//            // Unregister Broadcast Receiver
//            wifiP2pReceiver.unregisterReceiver();
//            wifiP2pReceiver = null;
//            receiverRegistrationButton.setText(getString(R.string.action_register_receiver));
//            displayToast(getString(R.string.status_receiver_unregistered));
//        }
//    }

    private void registerService() {
        if (wifiDirectHandler.isWifiEnabled()) {
            // Wi-Fi is enabled, continue registration
            if (wifiP2pManager != null && wifiP2pChannel != null) {
                // Wi-Fi Direct is registered, start Service registration
                startServiceRegistration();
            } else {
                // Wi-Fi Direct hasn't been registered
                displayToast(getString(R.string.warning_register_service_wifi_direct));
            }
        } else {
            // Wi-Fi hasn't been enabled
            displayToast(getString(R.string.warning_register_service_wifi));
        }
    }

    private void unregisterService() {
        if (wifiP2pService != null) {
            // Unregister Service
            wifiP2pManager.clearLocalServices(wifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Local Service successfully unregistered
                    wifiP2pService = null;
                    serviceRegistrationButton.setText(getString(R.string.action_register_service));
                    displayToast(getString(R.string.status_service_unregistered));
                }

                @Override
                public void onFailure(int error) {
                    // Failed to unregister Local Service
                    displayToast(getString(R.string.warning_service_unregistration_failed));
                }
            });
        }
    }

    private void startServiceRegistration() {
        // Register Local Service
        // Create a string map containing information about your service.
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        wifiP2pService = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        wifiP2pManager.addLocalService(wifiP2pChannel, wifiP2pService, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Local Service registered successfully
                serviceRegistrationButton.setText(getString(R.string.action_unregister_service));
                displayToast(getString(R.string.status_service_registered));
            }

            @Override
            public void onFailure(int error) {
                // Failed to register Local Service
                displayToast(getString(R.string.warning_service_registration_failed));
            }
        });
    }

    private void discoverServices(){
        if (wifiDirectHandler.isWifiEnabled()) {
            // Wi-Fi is enabled, continue Service discovery
            if (wifiP2pManager != null && wifiP2pChannel != null) {
                // Wi-Fi Direct is enabled, continue Service discovery
                startActivity(new Intent(this, AvailableServicesActivity.class));
            } else {
                // Wi-Fi Direct hasn't been registered
                displayToast(getString(R.string.warning_discover_service_wifi_direct));
            }
        } else {
            // Wi-Fi hasn't been enabled
            displayToast(getString(R.string.warning_discover_service_wifi));
        }
    }

    public void displayToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
        Log.i(getString(R.string.log_tag), message);
    }

    private ServiceConnection wifiServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WifiDirectHandler.WifiTesterBinder binder = (WifiDirectHandler.WifiTesterBinder) service;

            wifiDirectHandler = binder.getService();
            wifiDirectHandlerBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiDirectHandlerBound = false;
        }
    };
}
