package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.HashMap;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.ServiceData;
import edu.rit.se.crashavoidance.wifi.ServiceType;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * The Main Fragment of the application, which contains the Switches and Buttons to perform P2P tasks
 */
public class MainFragment extends Fragment {

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;
    private Switch toggleWifiSwitch;
    private Switch serviceRegistrationSwitch;
    private Switch noPromptServiceRegistrationSwitch;
    private Button discoverServicesButton;
    private AvailableServicesFragment availableServicesFragment;
    private DeviceInfoFragment deviceInfoFragment;
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

        updateToggles();

        // Set Toggle Listener for Wi-Fi Switch
        toggleWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Enable or disable Wi-Fi when Switch is toggled
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(WifiDirectHandler.LOG_TAG, "\nWi-Fi Switch Toggled");
                if(getHandler().isWifiEnabled()) {
                    // Disable Wi-Fi, disable all switches and buttons
                    toggleWifiSwitch.setChecked(false);
                    serviceRegistrationSwitch.setChecked(false);
                    noPromptServiceRegistrationSwitch.setChecked(false);
                    getHandler().setWifiEnabled(false);
                    serviceRegistrationSwitch.setEnabled(false);
                    noPromptServiceRegistrationSwitch.setEnabled(false);
                    discoverServicesButton.setEnabled(false);
                } else {
                    // Enable Wi-Fi, enable all switches and buttons
                    toggleWifiSwitch.setChecked(true);
                    getHandler().setWifiEnabled(true);
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
                    getHandler().startAddingLocalService(serviceData);
                    noPromptServiceRegistrationSwitch.setEnabled(false);
                } else {
                    // Remove local service
                    getHandler().removeService();
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
                    getHandler().startAddingNoPromptService(serviceData);
                    serviceRegistrationSwitch.setEnabled(false);
                } else {
                    // Remove no-prompt local service
                    getHandler().removeService();
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
                if (deviceInfoFragment == null) {
                    deviceInfoFragment = new DeviceInfoFragment();
                }
                mainActivity.addFragment(deviceInfoFragment);
            }
        });

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
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }

    /**
     * Shortcut for accessing the wifi handler
     */
    private WifiDirectHandler getHandler() {
        return wifiDirectHandlerAccessor.getWifiHandler();
    }

    private void updateToggles() {
        // Set state of Switches and Buttons on load
        if(getHandler().isWifiEnabled()) {
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
        Log.i(WifiDirectHandler.LOG_TAG, "Updating toggle switches");
    }
}
