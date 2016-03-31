package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.WiFiP2pService;

/**
 * Created by Brett on 3/16/2016.
 */
public class AvailableServicesListViewAdapter extends BaseAdapter {

    private List<WiFiP2pService> serviceList;
    private AvailableServicesActivity context;

    public AvailableServicesListViewAdapter(AvailableServicesActivity context, List<WiFiP2pService> serviceList) {
        this. serviceList = serviceList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return serviceList.size();
    }

    @Override
    public WiFiP2pService getItem(int position) {
        return serviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final WiFiP2pService service = getItem(position);

        // This will inflate the template view inside each ListView item
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.service_item, parent, false);
        }

        TextView instanceName = (TextView) convertView.findViewById(R.id.instanceName);
        TextView deviceName = (TextView) convertView.findViewById(R.id.deviceName);
        //TODO: This will need updates once real devices are in use
        instanceName.setText(service.instanceName);

        deviceName.setText(service.device.deviceName);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.onServiceClick(service);
            }
        });

        return convertView;
    }
}
