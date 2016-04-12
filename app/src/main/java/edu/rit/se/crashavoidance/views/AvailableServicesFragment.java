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


    private void setServiceList() {
        serviceListAdapter = new AvailableServicesListViewAdapter((MainActivity) getActivity(), services);
        setListAdapter(serviceListAdapter);
//        getListView().setOnItemClickListener(null);
    }

    private void startDiscoveringServices() {
        WifiDirectReceiver receiver = new WifiDirectReceiver();
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
            if (intent.getAction().equals(WifiDirectHandler.Event.DNS_SD_SERVICE_AVAILABLE.toString()))
            {
                String serviceKey = intent.getParcelableExtra(wifiDirectHandlerAccessor.getWifiHandler().getSERVICE_MAP_KEY());
                Log.d(LOG_TAG, "Service Key: " + serviceKey);
                DnsSdService service = wifiDirectHandlerAccessor.getWifiHandler().getDnsSdServiceMap().get(serviceKey);
                serviceListAdapter.addUnique(service);
                Log.d(LOG_TAG, "Found service for device " + service.getSrcDevice().deviceName);
                // TODO Capture an intent that indicates the peer list has changed
                // and see if we need to remove anything from our list
            }
        }
    }

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
