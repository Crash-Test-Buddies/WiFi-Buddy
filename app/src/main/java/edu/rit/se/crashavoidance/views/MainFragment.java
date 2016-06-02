package edu.rit.se.crashavoidance.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.HashMap;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.ServiceData;
import edu.rit.se.crashavoidance.wifi.ServiceType;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * The Main Fragment of the application, which contains the Switches and Buttons to perform P2P tasks
 */
public class MainFragment extends Fragment {

    private WifiDirectHandler wifiDirectHandler;
    private Switch toggleWifiSwitch;
    private Switch serviceRegistrationSwitch;
    private Switch noPromptServiceRegistrationSwitch;
    private Button discoverServicesButton;
    private TextView thisDeviceTextView;
    private AvailableServicesFragment availableServicesFragment;
    private MainActivity mainActivity;

    /**
     * Sets the layout for the UI, initializes the Buttons and Switches, and returns the View
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Sets the Layout for the UI
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Initialize Switches
        toggleWifiSwitch = (Switch) view.findViewById(R.id.toggleWifiSwitch);
        serviceRegistrationSwitch = (Switch) view.findViewById(R.id.serviceRegistrationSwitch);
        noPromptServiceRegistrationSwitch = (Switch) view.findViewById(R.id.noPromptServiceRegistrationSwitch);

        // Initialize Discover Services Button
        discoverServicesButton = (Button) view.findViewById(R.id.discoverServicesButton);

        // Set state of Switches and Buttons on load
        if(wifiDirectHandler.isWifiEnabled()) {
            toggleWifiSwitch.setChecked(true);
            serviceRegistrationSwitch.setEnabled(true);
            noPromptServiceRegistrationSwitch.setEnabled(true);
            discoverServicesButton.setEnabled(true);
        } else {
            toggleWifiSwitch.setChecked(false);
            serviceRegistrationSwitch.setChecked(false);
            noPromptServiceRegistrationSwitch.setChecked(false);
            serviceRegistrationSwitch.setEnabled(false);
            noPromptServiceRegistrationSwitch.setEnabled(false);
            discoverServicesButton.setEnabled(false);
        }

        // Set Toggle Listener for Wi-Fi Switch
        toggleWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Enable or disable Wi-Fi when Switch is toggled
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(WifiDirectHandler.LOG_TAG, "\nWi-Fi Switch Toggled");
                if(wifiDirectHandler.isWifiEnabled()) {
                    // Disable Wi-Fi, disable all switches and buttons
                    toggleWifiSwitch.setChecked(false);
                    serviceRegistrationSwitch.setChecked(false);
                    noPromptServiceRegistrationSwitch.setChecked(false);
                    wifiDirectHandler.setWifiEnabled(false);
                    serviceRegistrationSwitch.setEnabled(false);
                    noPromptServiceRegistrationSwitch.setEnabled(false);
                    discoverServicesButton.setEnabled(false);
                } else {
                    // Enable Wi-Fi, enable all switches and buttons
                    toggleWifiSwitch.setChecked(true);
                    wifiDirectHandler.setWifiEnabled(true);
                    serviceRegistrationSwitch.setEnabled(true);
                    discoverServicesButton.setEnabled(true);
                    noPromptServiceRegistrationSwitch.setEnabled(true);
                }
            }
        });

        // Set Toggle Listener for Service Registration Switch
        serviceRegistrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Add or Remove a Local Service when Switch is toggled
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(WifiDirectHandler.LOG_TAG, "\nService Registration Switch Toggled");
                if (isChecked) {
                    // Add local service
                    ServiceData serviceData = new ServiceData(
                            "Wi-Fi Direct Handler",         // Name
                            4545,                           // Port
                            new HashMap<String, String>(),  // Record
                            ServiceType.PRESENCE_TCP        // Type
                    );
                    wifiDirectHandler.startAddingLocalService(serviceData);
                    noPromptServiceRegistrationSwitch.setEnabled(false);
                } else {
                    // Remove local service
                    wifiDirectHandler.removeService();
                    noPromptServiceRegistrationSwitch.setEnabled(true);
                }
            }
        });

        // Set Toggle Listener for No-Prompt Service Registration Switch
        noPromptServiceRegistrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Add or Remove a No-Prompt Local Service when Switch is toggled
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(WifiDirectHandler.LOG_TAG, "\nNo-Prompt Service Registration Switch Toggled");
                if (isChecked) {
                    // Add no-prompt local service
                    ServiceData serviceData = new ServiceData(
                            "Wi-Fi Direct Handler",         // Name
                            4545,                           // Port
                            new HashMap<String, String>(),  // Record
                            ServiceType.PRESENCE_TCP        // Type
                    );
                    wifiDirectHandler.startAddingNoPromptService(serviceData);
                    serviceRegistrationSwitch.setEnabled(false);
                } else {
                    // Remove no-prompt local service
                    wifiDirectHandler.removeService();
                    serviceRegistrationSwitch.setEnabled(true);
                }
            }
        });

        // Set Click Listener for Discover Services Button
        discoverServicesButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Show AvailableServicesFragment when Discover Services Button is clicked
             */
            @Override
            public void onClick(View v) {
                Log.i(WifiDirectHandler.LOG_TAG, "\nDiscover Services Button Pressed");
                if (availableServicesFragment == null) {
                    availableServicesFragment = new AvailableServicesFragment();
                }
                mainActivity.replaceFragment(availableServicesFragment);
            }
        });

        thisDeviceTextView = (TextView) view.findViewById(R.id.thisDeviceTextView);
        thisDeviceTextView.setText(wifiDirectHandler.getThisDeviceInfo());

        return view;
    }

    /**
     * Sets the Main Activity instance
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
    }

    /**
     * Sets the WifiDirectHandler instance when MainFragment is attached to MainActivity
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            WiFiDirectHandlerAccessor wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
            wifiDirectHandler = wifiDirectHandlerAccessor.getWifiHandler();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }

        // Set the receiver for moving to the chat fragment
        ChatReceiver receiver = new ChatReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiDirectHandler.Action.SERVICE_CONNECTED);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiDirectHandler.Action.DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);
    }

    /**
     * Receiver for receiving intents from the WifiDirectHandler to update UI
     * when Wi-Fi Direct commands are completed
     */
    public class ChatReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found

            if (intent.getAction().equals(WifiDirectHandler.Action.SERVICE_CONNECTED)
               || intent.getAction().equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) ) {
                Log.i(WifiDirectHandler.LOG_TAG, "FRAGMENT SWITCH: Connected to service");
                ChatFragment newFrag = new ChatFragment();
                wifiDirectHandler.setChatFragment(newFrag);
                mainActivity.replaceFragment(newFrag);
            } else if (intent.getAction().equals(WifiDirectHandler.Action.DEVICE_CHANGED)) {
                thisDeviceTextView.setText(wifiDirectHandler.getThisDeviceInfo());
            }
        }
    }
}
