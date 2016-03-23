package edu.rit.se.crashavoidance;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import edu.rit.se.crashavoidance.views.AvailableServicesActivity;
import edu.rit.se.crashavoidance.views.LogsActivity;

public class initActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private Menu menu;
    private Button toggleWifiButton;
    private Button toggleWifiDirectRegistrationButton;
    private Button createServiceButton;
    private Button scanServicesButton;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;

    static final int SERVER_PORT = 4545;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        Toolbar toolbar = (Toolbar) findViewById(R.id.initToolbar);
        setSupportActionBar(toolbar);

        // Wi-Fi
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        toggleWifiButton = (Button) findViewById(R.id.wifiToggle_btn);
        toggleWifiDirectRegistrationButton = (Button) findViewById(R.id.wifiDirectRegister_btn);
        createServiceButton = (Button) findViewById(R.id.registerService_btn);
        scanServicesButton = (Button) findViewById(R.id.scanForServices_btn);

        if(wifiManager.isWifiEnabled()){
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
        } else {
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Adds Main Menu to the ActionBar
        getMenuInflater().inflate(R.menu.main_menu, menu);

        this.menu = menu;
        MenuItem toggleWifiMenuItem = menu.findItem(R.id.action_toggle_wifi);

        if(wifiManager.isWifiEnabled()){
            toggleWifiMenuItem.setTitle(getString(R.string.action_disable_wifi));
        } else {
            toggleWifiMenuItem.setTitle(getString(R.string.action_enable_wifi));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    public void onClickButtonToggleWifi(View view) {
        toggleWifi();
    }

    public void onClickButtonToggleWifiDirectRegistration(View view) {
        if (wifiP2pManager == null && wifiP2pChannel == null) {
            wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
            wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
            toggleWifiDirectRegistrationButton.setText(getString(R.string.action_unregister_wifi_direct));
            displayToast(getString(R.string.status_wifi_direct_initialized));
        } else {
            wifiP2pManager = null;
            wifiP2pChannel = null;
            toggleWifiDirectRegistrationButton.setText(getString(R.string.action_register_wifi_direct));
            displayToast(getString(R.string.status_wifi_direct_unregistered));
        }
    }

    public void onClickButtonRegisterService(View view) {
        if (wifiP2pManager != null && wifiP2pChannel != null) {
            startServiceRegistration();
        } else {
            displayToast(getString(R.string.warning_service_registration_failed));
        }
    }

    public void onClickButtonScanServices(View view) {
        scanForServices();
    }

    public void onClickMenuDisconnect(MenuItem item) {
        displayToast("Disconnect tapped");
    }

    public void onClickMenuToggleWifi(MenuItem item) {
        toggleWifi();
    }

    public void onClickMenuViewLogs(MenuItem item) {
        // Open the View Logs Activity
        Intent intent = new Intent(this, LogsActivity.class);
        startActivity(intent);
    }

    public void onClickMenuExit(MenuItem item) {
        finish();
    }

    private void toggleWifi(){
        MenuItem toggleWifiMenuItem = menu.findItem(R.id.action_toggle_wifi);
        if(wifiManager.isWifiEnabled()){
            // Disable Wi-Fi
            wifiManager.setWifiEnabled(false);
            displayToast(getString(R.string.status_wifi_disabled));
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
            toggleWifiMenuItem.setTitle(getString(R.string.action_enable_wifi));
        } else {
            // Enable Wi-Fi
            wifiManager.setWifiEnabled(true);
            displayToast(getString(R.string.status_wifi_enabled));
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
            toggleWifiMenuItem.setTitle(getString(R.string.action_disable_wifi));
        }
    }

    /**
     * Registers a local service
     */
    private void startServiceRegistration() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        wifiP2pManager.addLocalService(wifiP2pChannel, service, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                displayToast(getString(R.string.status_service_registered));
            }

            @Override
            public void onFailure(int error) {
                displayToast(getString(R.string.warning_service_registration_failed));
            }
        });

        //discoverService();
    }

    private void scanForServices(){
        Intent intent = new Intent(this, AvailableServicesActivity.class);
        startActivity(intent);
    }

    public void displayToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
