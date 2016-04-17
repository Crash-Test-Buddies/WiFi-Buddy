package edu.rit.se.crashavoidance.views;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.DnsSdService;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

public class AvailableServicesFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;
    private IntentFilter filter;

    List<DnsSdService> services = new ArrayList<DnsSdService>();
    AvailableServicesListViewAdapter serviceListAdapter;
    private WifiDirectHandler wiFiDirectHandler;
    AvailableServicesListViewAdapter servicesListAdapter;
    MainActivity mainActivity;
    private final String LOG_TAG = "AvailableServicesFrag";
    WifiDirectReceiver receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_available_services, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
    }

    /**
     * Set the service list adapter to display available services
     */
    private void setServiceList() {
        serviceListAdapter = new AvailableServicesListViewAdapter((MainActivity) getActivity(), services);
        setListAdapter(serviceListAdapter);
    }

    /**
     * Register the receiver to listen for the intents broadcasted by WifiDirectHandler
     * and call service discovery
     */
    private void startDiscoveringServices() {
        receiver = new WifiDirectReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiDirectHandler.Event.DNS_SD_SERVICE_AVAILABLE.toString());
        filter.addAction(WifiDirectHandler.Event.PEERS_CHANGED.toString());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);
        // Start discovering services through the WifiDirectHandler service
        wifiDirectHandlerAccessor.getWifiHandler().startDiscoveringServices();
        // Request peers through WifiDirectHandler service so we can see if the
        // peers in our list are still available
//        wifiDirectHandlerAccessor.getWifiHandler().requestPeers();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

        /**
     * Receiver for receiving intents from the WifiDirectHandler to update UI
     * when Wifi Direct commands are completed
     */
    public class WifiDirectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found
            if (intent.getAction().equals(WifiDirectHandler.Event.DNS_SD_SERVICE_AVAILABLE.toString()))
            {
                String serviceKey = intent.getStringExtra(wifiDirectHandlerAccessor.getWifiHandler().getSERVICE_MAP_KEY());
                DnsSdService service = wifiDirectHandlerAccessor.getWifiHandler().getDnsSdServiceMap().get(serviceKey);
                // Add the service to the UI and update
                serviceListAdapter.addUnique(service);
                Log.d(LOG_TAG, "Found service for device " + service.getSrcDevice().deviceName);
                // TODO Capture an intent that indicates the peer list has changed
                // and see if we need to remove anything from our list
            } else if (intent.getAction().equals(WifiDirectHandler.Event.DNS_SD_SERVICE_AVAILABLE.toString())){
                Log.d(LOG_TAG, "Peers list changed, loop over the list");
                WifiP2pDeviceList peers = intent.getParcelableExtra(wifiDirectHandlerAccessor.getWifiHandler().getPEERS());
                // Need to iterate over the collection. There is a get for the list but it is not compatible with Android 4.1-4.2
                Collection<WifiP2pDevice> devices = peers.getDeviceList();
                // Loop over each service to see if that device is in the peer list. We do this as there is no way of
                // knowing whether the service is still available or not, but we can see if the peer is available. If the peer
                // is not available then the service must not be available either
//                for (DnsSdService service : services){
//                    boolean remove = true;
//                    // Loop over the peer list
//                    for (WifiP2pDevice device : devices){
//                        // If we find the device address the peer is still available so set remove to false
//                        if (device.deviceAddress.equals(service.getSrcDevice().deviceAddress)){
//                            remove = false;
//                            break;
//                        }
//                    }
//                    // If the device address was not found, remove from the list
//                    if (remove){
//                        Log.d(LOG_TAG, "Removing device with address " + service.getSrcDevice().deviceAddress);
//                        services.remove(service);
//                        serviceListAdapter.notifyDataSetChanged();
//                    }
//                }
//                Log.d(LOG_TAG, "Finished checking peers list");
            // Remove the service from the service list if we get a service removed intent form the wifiDirectHandler
            } else if (intent.getAction().equals(WifiDirectHandler.Event.DEVICE_SERVICE_REMOVED.toString())){
                String deviceAddress = intent.getStringExtra(wiFiDirectHandler.getSERVICE_MAP_KEY());
                serviceListAdapter.removeService(wiFiDirectHandler.getDnsSdServiceMap().get(deviceAddress));
            }
        }
    }

    /**
     * This is called when the Fragment is opened and is attached to MainActivity
     * Sets the ListAdapter for the Service List and initiates the service discovery
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
            setServiceList();
            startDiscoveringServices();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }
}
