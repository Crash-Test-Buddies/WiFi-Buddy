package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;


public class MainFragment extends Fragment {

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;

    // Buttons
    private Button toggleWifiButton;
    private Button receiverRegistrationButton;
    private Button wifiDirectRegistrationButton;
    private Button serviceRegistrationButton;
    private Button discoverServicesButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Initialize Buttons
        toggleWifiButton = (Button) view.findViewById(R.id.toggleWifiButton);
        toggleWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                WifiDirectHandler handler = wifiDirectHandlerAccessor.getWifiHandler();
                if(wifiDirectHandlerAccessor.getWifiHandler().isWifiEnabled()) {
                    wifiDirectHandlerAccessor.getWifiHandler().setWifiEnabled(false);
                    toggleWifiButton.setText("Enable Wifi");
                } else {
                    wifiDirectHandlerAccessor.getWifiHandler().setWifiEnabled(true);
                    toggleWifiButton.setText("Disable Wifi");
                }
            }
        });
        wifiDirectRegistrationButton = (Button) view.findViewById(R.id.wifiDirectRegistrationButton);
        wifiDirectRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
            }
        });
        receiverRegistrationButton = (Button) view.findViewById(R.id.receiverRegistrationButton);
        receiverRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
            }
        });
        serviceRegistrationButton = (Button) view.findViewById(R.id.serviceRegistrationButton);
        serviceRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
            }
        });
        discoverServicesButton = (Button) view.findViewById(R.id.discoverServicesButton);
        discoverServicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }
}
