package edu.rit.se.crashavoidance.views;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.ClientSocketHandler;
import edu.rit.se.crashavoidance.wifi.DnsSdService;
import edu.rit.se.crashavoidance.wifi.OwnerSocketHandler;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * The main Activity of the application, which is a container for Fragments and the ActionBar
 * Also contains the WifiDirectHandler
 */
public class MainActivity extends AppCompatActivity implements WiFiDirectHandlerAccessor {

    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;
    private ChatFragment chatFragment = null;

    private LogsDialogFragment logsDialogFragment;
    private DeviceInfoFragment deviceInfoFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(WifiDirectHandler.LOG_TAG, "Creating MainActivity");
        setContentView(R.layout.activity_main);

        // Initialize ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.initToolbar);
        setSupportActionBar(toolbar);

        //Set the receiver for moving to the chat fragment
        ChatReceiver receiver = new ChatReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiDirectHandler.Action.SERVICE_CONNECTED);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        Log.i(WifiDirectHandler.LOG_TAG, "MainActivity created");
    }

    /**
     * Adds the Main Menu to the ActionBar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Called when a MenuItem in the Main Menu is selected
     * @param item Item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_logs:
                // View Logs MenuItem tapped
                if (logsDialogFragment == null) {
                    logsDialogFragment = new LogsDialogFragment();
                }
                logsDialogFragment.show(getFragmentManager(), "dialog");
                return true;
            case R.id.action_exit:
                // Exit MenuItem tapped
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * TODO add comment
     */
    private ServiceConnection wifiServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WifiDirectHandler.WifiTesterBinder binder = (WifiDirectHandler.WifiTesterBinder) service;

            wifiDirectHandler = binder.getService();
            wifiDirectHandlerBound = true;
            Log.i(WifiDirectHandler.LOG_TAG, "WifiDirectHandler service bound");

            // Add MainFragment to the 'fragment_container' when wifiDirectHandler is bound
            MainFragment mainFragment = new MainFragment();
            replaceFragment(mainFragment);
            deviceInfoFragment = new DeviceInfoFragment();
            addFragment(deviceInfoFragment);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiDirectHandlerBound = false;
            Log.i(WifiDirectHandler.LOG_TAG, "WifiDirectHandler service unbound");
        }
    };

    /**
     * Adds a Fragment to the 'fragment_container'
     * @param fragment Fragment to add
     */
    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    public void addFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, fragment);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * Returns the wifiDirectHandler
     * @return The wifiDirectHandler
     */
    @Override
    public WifiDirectHandler getWifiHandler() {
        return wifiDirectHandler;
    }

    public void onServiceClick(DnsSdService service) {
        Log.i(WifiDirectHandler.LOG_TAG, "\nService List item tapped");
        wifiDirectHandler.initiateConnectToService(service);
    }

    protected void onPause() {
        super.onPause();
        Log.i(WifiDirectHandler.LOG_TAG, "Pausing MainActivity");
        if (wifiDirectHandlerBound) {
            Log.i(WifiDirectHandler.LOG_TAG, "WifiDirectHandler service unbound");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
        Log.i(WifiDirectHandler.LOG_TAG, "MainActivity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(WifiDirectHandler.LOG_TAG, "Resuming MainActivity");
        Intent intent = new Intent(this, WifiDirectHandler.class);
        if(!wifiDirectHandlerBound) {
            bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
        }
        Log.i(WifiDirectHandler.LOG_TAG, "MainActivity resumed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(WifiDirectHandler.LOG_TAG, "Starting MainActivity");
        Intent intent = new Intent(this, WifiDirectHandler.class);
        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
        Log.i(WifiDirectHandler.LOG_TAG, "MainActivity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(WifiDirectHandler.LOG_TAG, "Stopping MainActivity");
        if(wifiDirectHandlerBound) {
            Intent intent = new Intent(this, WifiDirectHandler.class);
            stopService(intent);
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
        Log.i(WifiDirectHandler.LOG_TAG, "MainActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(WifiDirectHandler.LOG_TAG, "Destroying MainActivity");
        if (wifiDirectHandlerBound) {
            Log.i(WifiDirectHandler.LOG_TAG, "WifiDirectHandler service unbound");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
            Log.i(WifiDirectHandler.LOG_TAG, "MainActivity destroyed");
        }
    }

    /**
     * Receiver for receiving intents from the WifiDirectHandler to update UI
     * when Wi-Fi Direct commands are completed
     */

    public class ChatReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found
            if (intent.getAction().equals(WifiDirectHandler.Action.SERVICE_CONNECTED)
                    || intent.getAction().equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) ) {
                Log.i(WifiDirectHandler.LOG_TAG, "FRAGMENT SWITCH: Connected to service");
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                wifiDirectHandler.setChatFragment(chatFragment);
                replaceFragment(chatFragment);
                if (deviceInfoFragment == null) {
                    deviceInfoFragment = new DeviceInfoFragment();
                }
                addFragment(deviceInfoFragment);
            } else if (intent.getAction().equals(WifiDirectHandler.Action.DEVICE_CHANGED)) {
                deviceInfoFragment.getThisDeviceInfoTextView().setText(wifiDirectHandler.getThisDeviceInfo());
            }
        }
    }
}
