package edu.rit.se.crashavoidance.views;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.DnsSdService;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

public class AvailableServicesFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;

    List<DnsSdService> services = new ArrayList<DnsSdService>();
    AvailableServicesListViewAdapter serviceListAdapter;
    MainActivity mainActivity;
    private final String LOG_TAG = "AvailableServicesFrag";
    WifiDirectReceiver receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_available_services, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        startDiscoveringServices();
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
//        getListView().setOnItemClickListener(null);
    }

    /**
     * Register the receiver to listen for the intents broadcasted by WifiDirectHandler
     * and call service discovery
     */
    private void startDiscoveringServices() {
        receiver = new WifiDirectReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiDirectHandler.Event.DNS_SD_SERVICE_AVAILABLE.toString());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);
        wifiDirectHandlerAccessor.getWifiHandler().startDiscoveringServices();
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
