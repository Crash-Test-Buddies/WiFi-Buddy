package edu.rit.se.crashavoidance;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

import edu.rit.se.crashavoidance.views.MainActivity;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private edu.rit.se.crashavoidance.views.MainActivity mainActivity;

    private IntentFilter intentFilter = null;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       Activity activity) {
        super();
        this.wifiP2pManager = manager;
        this.wifiP2pChannel = channel;
        this.mainActivity = (MainActivity) activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            // Sticky Intent
            handleWifiP2pStateChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            handleWifiP2pPeersChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            handleWifiP2pConnectionChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            // Sticky Intent
            handleWifiP2pThisDeviceChanged(intent);
        }
    }

    private void handleWifiP2pStateChanged(Intent intent) {

    }

    private void handleWifiP2pPeersChanged(Intent intent) {

    }

    private void handleWifiP2pConnectionChanged(Intent intent) {

    }

    private void handleWifiP2pThisDeviceChanged(Intent intent) {

    }

    public void registerReceiver() {
        mainActivity.registerReceiver(this, getIntentFilter());
    }

    public void unregisterReceiver() {
        mainActivity.unregisterReceiver(this);
    }

    public IntentFilter getIntentFilter() {
        if (intentFilter == null) {
            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        }

        return intentFilter;
    }
}
