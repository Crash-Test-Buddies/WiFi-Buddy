package edu.rit.se.crashavoidance.views;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.widget.TextView;
import android.widget.Toast;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.wifibuddy.DnsSdService;
import edu.rit.se.wifibuddy.WifiDirectHandler;

/**
 * The main Activity of the application, which is a container for Fragments and the ActionBar.
 * Contains WifiDirectHandler, which is a service
 * MainActivity has a Communication BroadcastReceiver to handle Intents fired from WifiDirectHandler.
 */
public class MainActivity extends AppCompatActivity implements WiFiDirectHandlerAccessor {

    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;
    private ChatFragment chatFragment = null;
    private LogsDialogFragment logsDialogFragment;
    private TextView deviceInfoTextView;
    private static final String TAG = WifiDirectHandler.TAG + "MainActivity";

    /**
     * Sets the UI layout for the Activity.
     * Registers a Communication BroadcastReceiver so the Activity can be notified of
     * intents fired in WifiDirectHandler, like Service Connected and Messaged Received.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Creating MainActivity");
        setContentView(R.layout.activity_main);

        // Initialize ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        deviceInfoTextView = (TextView) findViewById(R.id.thisDeviceInfoTextView);

        // Set the CommunicationReceiver for receiving intents fired from the WifiDirectHandler
        // Used to update the UI and receive communication messages
        CommunicationReceiver communicationReceiver = new CommunicationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiDirectHandler.Action.SERVICE_CONNECTED);
        filter.addAction(WifiDirectHandler.Action.MESSAGE_RECEIVED);
        filter.addAction(WifiDirectHandler.Action.DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(communicationReceiver, filter);
        Log.i(TAG, "Communication Receiver registered");
        Log.i(TAG, "MainActivity created");
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
            case  R.id.database_view:
                Intent dbmanager = new Intent(this,AndroidDatabaseManager.class);
                startActivity(dbmanager);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO: BRETT, add JavaDoc
    // Note: This is used to run WifiDirectHandler as a Service instead of being coupled to an
    //          Activity. This is NOT a connection to a P2P service being broadcast from a device
    private ServiceConnection wifiServiceConnection = new ServiceConnection() {

        /**
         * Called when a connection to the Service has been established, with the IBinder of the
         * communication channel to the Service.
         * @param name The component name of the service that has been connected
         * @param service The IBinder of the Service's communication channel
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "ComponentName: " + name);
            Log.i(TAG, "Service: " + service);
            WifiDirectHandler.WifiTesterBinder binder = (WifiDirectHandler.WifiTesterBinder) service;

            wifiDirectHandler = binder.getService();
            wifiDirectHandlerBound = true;
            Log.i(TAG, "WifiDirectHandler service bound");

            // Add MainFragment to the 'fragment_container' when wifiDirectHandler is bound
            MainFragment mainFragment = new MainFragment();
            replaceFragment(mainFragment);

            deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
        }

        /**
         * Called when a connection to the Service has been lost.  This typically
         * happens when the process hosting the service has crashed or been killed.
         * This does not remove the ServiceConnection itself -- this
         * binding to the service will remain active, and you will receive a call
         * to onServiceConnected when the Service is next running.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiDirectHandlerBound = false;
            Log.i(TAG, "WifiDirectHandler service unbound");
        }
    };

    /**
     * Replaces a Fragment in the 'fragment_container'
     * @param fragment Fragment to add
     */
    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);

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

    /**
     * Initiates a P2P connection to a service when a Service ListItem is tapped.
     * An invitation appears on the other device to accept or decline the connection.
     * @param service The service to connect to
     */
    public void onServiceClick(DnsSdService service) {
        Log.i(TAG, "\nService List item tapped");
        String sourceDeviceName = service.getSrcDevice().deviceName;
        if (sourceDeviceName.equals("")) {
            sourceDeviceName = "other device";
        }
        Toast.makeText(this, "Inviting " + sourceDeviceName + " to connect", Toast.LENGTH_LONG).show();
        // TODO: maybe make it so that if you are already connected then go to chat
        wifiDirectHandler.initiateConnectToService(service);
    }

    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Pausing MainActivity");
        if (wifiDirectHandlerBound) {
            Log.i(TAG, "WifiDirectHandler service unbound");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
        Log.i(TAG, "MainActivity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Resuming MainActivity");
        Intent intent = new Intent(this, WifiDirectHandler.class);
        if(!wifiDirectHandlerBound) {
            bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
        }
        Log.i(TAG, "MainActivity resumed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Starting MainActivity");
        Intent intent = new Intent(this, WifiDirectHandler.class);
        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "MainActivity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Stopping MainActivity");
        if(wifiDirectHandlerBound) {
            Intent intent = new Intent(this, WifiDirectHandler.class);
            stopService(intent);
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
        Log.i(TAG, "MainActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying MainActivity");
        if (wifiDirectHandlerBound) {
            Log.i(TAG, "WifiDirectHandler service unbound");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
            Log.i(TAG, "MainActivity destroyed");
        }
    }

    /**
     * BroadcastReceiver used to receive Intents fired from the WifiDirectHandler when P2P events occur
     * Used to update the UI and receive communication messages
     */
    public class CommunicationReceiver extends BroadcastReceiver {

        private static final String TAG = WifiDirectHandler.TAG + "CommReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found
            if (intent.getAction().equals(WifiDirectHandler.Action.SERVICE_CONNECTED)) {
                Log.i(TAG, "Service connected");
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                replaceFragment(chatFragment);
                Log.i(TAG, "Switching to Chat fragment");
            } else if (intent.getAction().equals(WifiDirectHandler.Action.DEVICE_CHANGED)) {
                Log.i(TAG, "This device changed");
                deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
            } else if (intent.getAction().equals(WifiDirectHandler.Action.MESSAGE_RECEIVED)) {
                Log.i(TAG, "Message received");
                if(chatFragment != null) {
                    chatFragment.pushMessage(intent.getByteArrayExtra(WifiDirectHandler.MESSAGE_KEY));
                }
            }
        }
    }
}
