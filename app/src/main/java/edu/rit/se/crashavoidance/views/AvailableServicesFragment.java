package edu.rit.se.crashavoidance.views;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
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
    List<DnsSdService> services = new ArrayList<DnsSdService>();
    AvailableServicesListViewAdapter serviceListAdapter;
    MainActivity mainActivity;
    WifiDirectHandler handler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_available_services, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startDiscoveringServices();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        setServiceList();
    }

    private void setServiceList() {
        //ListView listView = (ListView) findViewById(R.id.availableServicesList);
        serviceListAdapter = new AvailableServicesListViewAdapter((MainActivity) getActivity(), services);
        setListAdapter(serviceListAdapter);
        getListView().setOnItemClickListener(null);
    }

    private void startDiscoveringServices() {
        //mainActivity.getWifiHandler().startDiscoveringServices();
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
                String serviceKey = intent.getParcelableExtra("dnsSdServiceKey");
                DnsSdService service = handler.getDnsSdServiceMap().get(serviceKey);
                serviceListAdapter.addUnique(service);
                // TODO Capture an intent that indicates the peer list has changed
                // and see if we need to remove anything from our list
            }
        }
    }
}
