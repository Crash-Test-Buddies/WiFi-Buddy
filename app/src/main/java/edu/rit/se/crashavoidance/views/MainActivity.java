package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.WiFiDirectBroadcastReceiver;

public class MainActivity extends AppCompatActivity {

    // Services
    private WifiManager wifiManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private WiFiDirectBroadcastReceiver wifiP2pReceiver;

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

    public WifiManager getWifiManger() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        }
        return wifiManager;
    }

    public WifiP2pManager getWifiP2pManger() {
        if (wifiP2pManager == null) {
            wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        }
        return wifiP2pManager;
    }

    public WifiP2pManager.Channel getWifiP2pChannel() {
        if (wifiP2pChannel == null) {
            wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        }
        return wifiP2pChannel;
    }

    public WiFiDirectBroadcastReceiver getWifiP2pReceiver() {
        if (wifiP2pReceiver == null) {
            wifiP2pReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, this);
        }
        return wifiP2pReceiver;
    }

    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

}
