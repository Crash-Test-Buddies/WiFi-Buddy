package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.HashMap;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.ServiceData;
import edu.rit.se.crashavoidance.wifi.ServiceType;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * The main Fragment of the application, which contains the buttons to perform P2P actions
 */
public class MainFragment extends Fragment {

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;
    private WifiDirectHandler wifiDirectHandler;
    private Button toggleWifiButton;
    AvailableServicesFragment availableServicesFragment;
    MainActivity mainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Initialize Toggle WiFi Button
        toggleWifiButton = (Button) view.findViewById(R.id.toggleWifiButton);
        toggleWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiDirectHandler.isWifiEnabled()) {
                    wifiDirectHandler.setWifiEnabled(false);
                    toggleWifiButton.setText(getString(R.string.action_enable_wifi));
                    toggleWifiButton.setBackgroundColor(Color.RED);
                } else {
                    wifiDirectHandler.setWifiEnabled(true);
                    toggleWifiButton.setText(getString(R.string.action_disable_wifi));
                    toggleWifiButton.setBackgroundColor(Color.GREEN);
                }
            }
        });
        if(wifiDirectHandler.isWifiEnabled()) {
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
            toggleWifiButton.setBackgroundColor(Color.GREEN);
        } else {
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
            toggleWifiButton.setBackgroundColor(Color.RED);
        }

        // Initialize Add Local Service Button
        Button serviceRegistrationButton = (Button) view.findViewById(R.id.serviceRegistrationButton);
        serviceRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceData serviceData = new ServiceData(
                    "wifiTester",
                    4545,
                    new HashMap<String, String>(),
                    ServiceType.PRESENCE_TCP
                );
                wifiDirectHandler.startAddingLocalService(serviceData);
            }
        });

        // Initialize Discover Services Button
        Button discoverServicesButton = (Button) view.findViewById(R.id.discoverServicesButton);
        discoverServicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (availableServicesFragment == null) {
                    availableServicesFragment = new AvailableServicesFragment();
                }
                mainActivity.replaceFragment(availableServicesFragment);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
            wifiDirectHandler = wifiDirectHandlerAccessor.getWifiHandler();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }
}
