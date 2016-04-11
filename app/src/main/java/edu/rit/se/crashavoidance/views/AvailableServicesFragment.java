package edu.rit.se.crashavoidance.views;


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

public class AvailableServicesFragment extends ListFragment implements AdapterView.OnItemClickListener {
    List<DnsSdService> services = new ArrayList<DnsSdService>();
    AvailableServicesListViewAdapter serviceListAdapter;
    MainActivity mainActivity;

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
}
