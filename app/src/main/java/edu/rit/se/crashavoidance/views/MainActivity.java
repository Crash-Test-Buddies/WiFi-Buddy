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
import android.view.Menu;
import android.view.MenuItem;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.DnsSdService;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * The main Activity of the application, which is a container for Fragments and
 * contains the Toolbar
 */
public class MainActivity extends AppCompatActivity implements WiFiDirectHandlerAccessor {

    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.initToolbar);
        setSupportActionBar(toolbar);

        // Check whether the activity is using the layout version with
        // the fragment_container FrameLayout. If so, we must add the first fragment
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of ExampleFragment
            MainFragment mainFragment = new MainFragment();

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mainFragment).commit();
        }
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
     * Adds Main Menu to the ActionBar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_logs:
                // View Logs MenuItem tapped
                LogsDialogFragment logsDialogFragment = new LogsDialogFragment();
                logsDialogFragment.show(getFragmentManager(), "dialog");
                return true;
            case R.id.action_exit:
                // Exit MenuItem tapped
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public WifiDirectHandler getWifiHandler() {
        return wifiDirectHandler;
    }

    public void onServiceClick(DnsSdService service) {
    }
}
