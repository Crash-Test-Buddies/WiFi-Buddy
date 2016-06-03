package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

public class DeviceInfoFragment extends Fragment {
    private WifiDirectHandler wifiDirectHandler;
    private TextView thisDeviceInfoTextView;

    public void setThisDeviceInfoTextView(TextView thisDeviceInfoTextView) {
        this.thisDeviceInfoTextView = thisDeviceInfoTextView;
    }

    public TextView getThisDeviceInfoTextView() {
        return thisDeviceInfoTextView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Sets the Layout for the UI
        final View view = inflater.inflate(R.layout.fragment_device_info, container, false);
        thisDeviceInfoTextView = (TextView) view.findViewById((R.id.thisDeviceInfoTextView));
        WifiP2pDevice thisDevice = wifiDirectHandler.getThisDevice();
        String thisDeviceInfo;
        if (thisDevice != null) {
            thisDeviceInfo = thisDevice.deviceName +
                    "\n" + wifiDirectHandler.deviceStatusToString(thisDevice.status);
        } else {
            thisDeviceInfo = "No Device Info";
        }
        thisDeviceInfoTextView.setText(thisDeviceInfo);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            WiFiDirectHandlerAccessor wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
            wifiDirectHandler = wifiDirectHandlerAccessor.getWifiHandler();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }
}
