package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.DnsSdService;
import edu.rit.se.crashavoidance.wifi.DnsSdTxtRecord;

/**
 *
 */
public class AvailableServicesListViewAdapter extends BaseAdapter {

    private List<DnsSdService> serviceList;
    private MainActivity context;

    public AvailableServicesListViewAdapter(MainActivity context, List<DnsSdService> serviceList) {
        this.context = context;
        this.serviceList = serviceList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final DnsSdService service = getItem(position);

        // Inflates the template view inside each ListView item
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.service_item, parent, false);
        }

        TextView instanceName = (TextView) convertView.findViewById(R.id.instanceName);
        TextView deviceInfo = (TextView) convertView.findViewById(R.id.deviceInfo);

        instanceName.setText(service.getInstanceName());

        String records = "";
        if (context.getWifiHandler() != null) {
            DnsSdTxtRecord txtRecord = context.getWifiHandler().getDnsSdTxtRecordMap().get(service.getSrcDevice().deviceAddress);
            if (txtRecord != null) {
                records = txtRecord.getRecord().toString();
            }
        }

        deviceInfo.setText(deviceToString(service.getSrcDevice()) + records);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.onServiceClick(service);
            }
        });

        return convertView;
    }

    /**
     * Add service to the Services list if it has not already been added
     * @param service Service to be added to list
     * @return false if item was already in the list
     */
    public Boolean addUnique(DnsSdService service) {
        if (serviceList.contains(service)) {
            return false;
        } else {
            serviceList.add(service);
            this.notifyDataSetChanged();
            return true;
        }
    }

    private String deviceToString(WifiP2pDevice device) {
        String strDevice = "  - Device address: " + device.deviceAddress
                + "\n  - Device name: " + device.deviceName
                + "\n  - Is group owner: " + device.isGroupOwner()
                + "\n  - Is Service Discoverable: " + device.isServiceDiscoveryCapable();

        int status = device.status;
        String strStatus;
        if (status == WifiP2pDevice.AVAILABLE) {
            strStatus = "Available";
        } else if (status == WifiP2pDevice.INVITED) {
            strStatus = "Invited";
        } else if (status == WifiP2pDevice.CONNECTED) {
            strStatus = "Connected";
        } else if (status == WifiP2pDevice.FAILED) {
            strStatus = "Failed";
        } else if (status == WifiP2pDevice.UNAVAILABLE) {
            strStatus = "Unavailable";
        } else {
            strStatus = "Unknown";
        }

        strDevice += "\n  - Status: " + strStatus + "\n";
        return strDevice;
    }

    @Override
    public int getCount() {
        return serviceList.size();
    }

    @Override
    public DnsSdService getItem(int position) {
        return serviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
