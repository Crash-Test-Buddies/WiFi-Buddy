package edu.rit.se.crashavoidance.views;

import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.WiFiP2pService;
import edu.rit.se.crashavoidance.wifi.DnsSdService;
import edu.rit.se.crashavoidance.wifi.WiFiDirectBroadcastReceiver;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * Created by Brett on 2/2/2016.
 */
public class ServicesList extends ListFragment {

    WiFiDevicesAdapter listAdapter = null;
    List<WiFiP2pService> serviceList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.devices_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        serviceList = new ArrayList<WiFiP2pService>();
        listAdapter = new WiFiDevicesAdapter(this.getActivity(),
                android.R.layout.simple_list_item_2, android.R.id.text1,
                serviceList);
        setListAdapter(listAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ((DeviceClickListener) getActivity()).connectP2p((WiFiP2pService) l
                .getItemAtPosition(position));
        ((TextView) v.findViewById(android.R.id.text2)).setText("Connecting");
    }

    private void initiateServiceDiscovery(){

    }

    interface DeviceClickListener {
        public void connectP2p(WiFiP2pService wifiP2pService);
    }

    /**
     * Receiver for receiving intents from the WifiDirectHandler to update UI
     * when Wifi Direct commands are completed
     */
    public class WifiDirectReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiDirectHandler.Event.DNS_SD_SERVICE_AVAILABLE.toString()))
            {
                // TODO Which service record type do we want to use?
                DnsSdService service = intent.getParcelableExtra("serviceRecord");
                //listAdapter.addUnique(service);
                // TODO Capture an intent that indicates the peer list has changed
                // and see if we need to remove anything from our list
            }
        }
    }
    public class WiFiDevicesAdapter extends ArrayAdapter<WiFiP2pService> {

        private List<WiFiP2pService> items;

        public WiFiDevicesAdapter(Context context, int resource,
                                  int textViewResourceId, List<WiFiP2pService> items) {
            super(context, resource, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_2, null);
            }
            WiFiP2pService service = items.get(position);
            if (service != null) {
                TextView nameText = (TextView) v
                        .findViewById(android.R.id.text1);

                if (nameText != null) {
                    nameText.setText(service.device.deviceName + " - " + service.instanceName);
                }
                TextView statusText = (TextView) v
                        .findViewById(android.R.id.text2);
                statusText.setText(getDeviceStatus(service.device.status));
            }
            return v;
        }

        /**
         * Add service to the Services list if it has not already been added
         * @param service Service to be added to list
         * @return false if item was already in the list
         */
        public Boolean addUnique(WiFiP2pService service) {
            if (items.contains(service)) {
                return false;
            } else {
                this.add(service);
                this.notifyDataSetChanged();
                return true;
            }
        }
    }

    public static String getDeviceStatus(int statusCode) {
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }


}
