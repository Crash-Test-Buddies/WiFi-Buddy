package edu.rit.se.crashavoidance.views;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.DnsSdService;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * The main Activity of the application, which is a container for Fragments and the ActionBar
 * Also contains the WifiDirectHandler
 */
public class MainActivity extends AppCompatActivity implements WiFiDirectHandlerAccessor {

    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;
    private LogsDialogFragment logsDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.initToolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, WifiDirectHandler.class);
        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(wifiDirectHandlerBound) {
            Intent intent = new Intent(this, WifiDirectHandler.class);
            stopService(intent);
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
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
            Log.i(wifiDirectHandler.LOG_TAG, "WifiDirectHandler bound");

            // Add MainFragment to the 'fragment_container' when wifiDirectHandler is bound
            MainFragment mainFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mainFragment).commit();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiDirectHandlerBound = false;
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

    /**
     * Returns the wifiDirectHandler
     * @return The wifiDirectHandler
     */
    @Override
    public WifiDirectHandler getWifiHandler() {
        return wifiDirectHandler;
    }

    public void onServiceClick(DnsSdService service) {
        wifiDirectHandler.connectToService(service);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wifiDirectHandlerBound) {
            Log.i("MainActivity","onPause -- unbindService");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, WifiDirectHandler.class);
        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (wifiDirectHandlerBound) {
            Log.i("MainActivity","onDestroy -- unbindService");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
        }
    }
}
