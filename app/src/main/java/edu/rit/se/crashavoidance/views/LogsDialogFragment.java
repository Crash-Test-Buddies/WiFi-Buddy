package edu.rit.se.crashavoidance.views;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * DialogFragment that shows a list of log messages
 */
public class LogsDialogFragment extends DialogFragment {

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;
    private WifiDirectHandler wifiDirectHandler;
    private TextView logTextView;

    /**
     * This is called when the Fragment is opened and is attached to MainActivity
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
            wifiDirectHandler = wifiDirectHandlerAccessor.getWifiHandler();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
        wifiDirectHandler.logMessage("Viewing logs");
        Log.i(WifiDirectHandler.LOG_TAG, "Viewing logs");
        AlertDialog.Builder dialogBuilder =  new  AlertDialog.Builder(getActivity())
            .setTitle(getString(R.string.title_logs))
            .setNegativeButton(getString(R.string.action_close),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }
            );

        LayoutInflater i = getActivity().getLayoutInflater();
        View rootView = i.inflate(R.layout.fragment_logs_dialog, null);

        logTextView = (TextView) rootView.findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        logTextView.setText(wifiDirectHandler.getLogs());

        dialogBuilder.setView(rootView);
        return dialogBuilder.create();
    }
}
