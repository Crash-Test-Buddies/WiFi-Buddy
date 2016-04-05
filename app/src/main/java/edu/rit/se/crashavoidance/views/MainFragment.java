package edu.rit.se.crashavoidance.views;

import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.WiFiDirectBroadcastReceiver;

public class MainFragment extends Fragment {

    // Buttons
    private Button toggleWifiButton;
    private Button receiverRegistrationButton;
    private Button wifiDirectRegistrationButton;
    private Button serviceRegistrationButton;
    private Button discoverServicesButton;

    private MainActivity mainActivity;
    // Services
    private WifiManager wifiManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private WifiP2pDnsSdServiceInfo wifiP2pService;
    private WiFiDirectBroadcastReceiver wifiP2pReceiver;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    static final int SERVER_PORT = 4545;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Main Activity, where the service managers are
        mainActivity = (MainActivity) getActivity();

        // Wi-Fi Service Manager
        wifiManager = mainActivity.getWifiManger();

        // Initialize Buttons
        toggleWifiButton = (Button) view.findViewById(R.id.toggleWifiButton);
        toggleWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onClickButtonToggleWifi(view);
            }
        });
        wifiDirectRegistrationButton = (Button) view.findViewById(R.id.wifiDirectRegistrationButton);
        wifiDirectRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onClickButtonWifiDirectRegistration(view);
            }
        });
        receiverRegistrationButton = (Button) view.findViewById(R.id.receiverRegistrationButton);
        receiverRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onClickButtonReceiverRegistration(view);
            }
        });
        serviceRegistrationButton = (Button) view.findViewById(R.id.serviceRegistrationButton);
        serviceRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onClickButtonServiceRegistration(view);
            }
        });
        discoverServicesButton = (Button) view.findViewById(R.id.discoverServicesButton);
        discoverServicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onClickButtonDiscoverServices(view);
            }
        });

        // Set Toggle Wi-Fi Button based on Wi-Fi state
        if(wifiManager.isWifiEnabled()){
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
        } else {
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
        }

        return view;
    }

    public void onClickButtonToggleWifi(View view) {
        String action = toggleWifiButton.getText().toString();
        if (action.equals(getString(R.string.action_enable_wifi))) {
            // Enable Wi-Fi
            enableWifi();
        } else if (action.equals(getString(R.string.action_disable_wifi))) {
            // Disable Wi-Fi and other dependent services (Service, Receiver, Wi-Fi Direct Registration)
            unregisterService();
            unregisterReceiver();
            unregisterWifiDirect();
            disableWifi();
        }
    }

    public void onClickButtonWifiDirectRegistration(View view) {
        String action = wifiDirectRegistrationButton.getText().toString();
        if (action.equals(getString(R.string.action_register_wifi_direct))) {
            // Register Wi-Fi Direct
            registerWifiDirect();
        } else if (action.equals(getString(R.string.action_unregister_wifi_direct))) {
            // Unregister Wi-Fi Direct, Unregister BroadcastReceiver, and Unregister Services
            unregisterService();
            unregisterReceiver();
            unregisterWifiDirect();
        }
    }

    public void onClickButtonReceiverRegistration(View view) {
        String action = receiverRegistrationButton.getText().toString();
        if (action.equals(getString(R.string.action_register_receiver))) {
            // Register Wi-Fi Direct BroadcastReceiver
            registerReceiver();
        } else if (action.equals(getString(R.string.action_unregister_receiver))) {
            // Unregister Wi-Fi Direct BroadcastReceiver
            unregisterReceiver();
        }
    }

    public void onClickButtonServiceRegistration(View view) {
        String action = serviceRegistrationButton.getText().toString();
        if (action.equals(getString(R.string.action_register_service))) {
            // Register Local Service
            registerService();
        } else if (action.equals(getString(R.string.action_unregister_service))) {
            // Unregister Local Service
            unregisterService();
        }
    }

    public void onClickButtonDiscoverServices(View view) {
        discoverServices();
    }

    private void enableWifi() {
        if (!wifiManager.isWifiEnabled()) {
            // Enable Wi-Fi
            wifiManager.setWifiEnabled(true);
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
            displayToast(getString(R.string.status_wifi_enabled));
        } else {
            // Failed to enable Wi-Fi - Wi-Fi is already enabled
            displayToast(getString(R.string.warning_wifi_already_enabled));
        }
    }

    private void disableWifi() {
        if (wifiManager.isWifiEnabled()) {
            // Disable Wi-Fi
            wifiManager.setWifiEnabled(false);
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
            displayToast(getString(R.string.status_wifi_disabled));
        } else {
            // Failed to disable Wi-Fi - Wi-Fi is already disabled
            displayToast(getString(R.string.warning_wifi_already_disabled));
        }
    }

    private void registerWifiDirect() {
        if (wifiManager.isWifiEnabled()) {
            // Wi-Fi is enabled, continue registration
            wifiP2pManager = mainActivity.getWifiP2pManger();
            wifiP2pChannel = mainActivity.getWifiP2pChannel();
            wifiDirectRegistrationButton.setText(getString(R.string.action_unregister_wifi_direct));
            displayToast(getString(R.string.status_wifi_direct_initialized));
        } else {
            // Wi-Fi isn't enabled, stop registration
            displayToast(getString(R.string.warning_wifi_direct_wifi_disabled));
        }
    }

    private void unregisterWifiDirect() {
        if (wifiP2pManager != null || wifiP2pChannel != null) {
            // Unregister Wi-Fi Direct
            wifiP2pManager = null;
            wifiP2pChannel = null;

            wifiDirectRegistrationButton.setText(getString(R.string.action_register_wifi_direct));
            displayToast(getString(R.string.status_wifi_direct_unregistered));
        }
    }

    private void registerReceiver() {
        if (wifiManager.isWifiEnabled()) {
            // Wi-Fi is enabled, continue registration
            if (wifiP2pManager != null && wifiP2pChannel != null) {
                // Wi-Fi Direct is registered, start Broadcast Receiver Registration
                wifiP2pReceiver = mainActivity.getWifiP2pReceiver();
                wifiP2pReceiver.registerReceiver();
                receiverRegistrationButton.setText(getString(R.string.action_unregister_receiver));
                displayToast(getString(R.string.status_receiver_registered));
            } else {
                // Wi-Fi Direct hasn't been registered
                displayToast(getString(R.string.warning_receiver_wifi_direct));
            }
        } else {
            // Wi-Fi hasn't been enabled
            displayToast(getString(R.string.warning_receiver_wifi));
        }
    }

    private void unregisterReceiver() {
        if (wifiP2pReceiver != null) {
            // Unregister Broadcast Receiver
            wifiP2pReceiver.unregisterReceiver();
            wifiP2pReceiver = null;
            receiverRegistrationButton.setText(getString(R.string.action_register_receiver));
            displayToast(getString(R.string.status_receiver_unregistered));
        }
    }

    private void registerService() {
        if (wifiManager.isWifiEnabled()) {
            // Wi-Fi is enabled, continue registration
            if (wifiP2pManager != null && wifiP2pChannel != null) {
                // Wi-Fi Direct is registered, start Service registration
                startServiceRegistration();
            } else {
                // Wi-Fi Direct hasn't been registered
                displayToast(getString(R.string.warning_register_service_wifi_direct));
            }
        } else {
            // Wi-Fi hasn't been enabled
            displayToast(getString(R.string.warning_register_service_wifi));
        }
    }

    private void unregisterService() {
        if (wifiP2pService != null) {
            // Unregister Service
            wifiP2pManager.clearLocalServices(wifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Local Service successfully unregistered
                    wifiP2pService = null;
                    serviceRegistrationButton.setText(getString(R.string.action_register_service));
                    displayToast(getString(R.string.status_service_unregistered));
                }

                @Override
                public void onFailure(int error) {
                    // Failed to unregister Local Service
                    displayToast(getString(R.string.warning_service_unregistration_failed));
                }
            });
        }
    }

    private void startServiceRegistration() {
        // Register Local Service
        // Create a string map containing information about your service.
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        wifiP2pService = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        wifiP2pManager.addLocalService(wifiP2pChannel, wifiP2pService, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Local Service registered successfully
                serviceRegistrationButton.setText(getString(R.string.action_unregister_service));
                displayToast(getString(R.string.status_service_registered));
            }

            @Override
            public void onFailure(int error) {
                // Failed to register Local Service
                displayToast(getString(R.string.warning_service_registration_failed));
            }
        });
    }

    private void discoverServices(){
        if (wifiManager.isWifiEnabled()) {
            // Wi-Fi is enabled, continue Service discovery
            if (wifiP2pManager != null && wifiP2pChannel != null) {
                // Wi-Fi Direct is enabled, continue Service discovery
                //startActivity(new Intent(this, AvailableServicesActivity.class));
                ServicesList servicesList = new ServicesList();
                mainActivity.replaceFragment(servicesList);
            } else {
                // Wi-Fi Direct hasn't been registered
                displayToast(getString(R.string.warning_discover_service_wifi_direct));
            }
        } else {
            // Wi-Fi hasn't been enabled
            displayToast(getString(R.string.warning_discover_service_wifi));
        }
    }

    public void displayToast(String message) {
        Toast toast = Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT);
        toast.show();
        Log.i(getString(R.string.log_tag), message);
    }
}
