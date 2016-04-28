package edu.rit.se.crashavoidance.views;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * DialogFragment that shows the WifiDirectHandler log messages
 */
public class LogsDialogFragment extends DialogFragment {

    private StringBuilder log = new StringBuilder();

    /**
     * Creates the AlertDialog, sets the WifiDirectHandler instance, and sets the logs TextView
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Sets the Layout for the UI
        LayoutInflater i = getActivity().getLayoutInflater();
        View rootView = i.inflate(R.layout.fragment_logs_dialog, null);

        // Sets the WifiDirectHandler instance
        WifiDirectHandler wifiDirectHandler;
        try {
            WiFiDirectHandlerAccessor wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
            wifiDirectHandler = wifiDirectHandlerAccessor.getWifiHandler();
            //Log.i(wifiDirectHandler.LOG_TAG, "Viewing logs");
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }

        // Sets the logs TextView and shows the WifiDirectHandler logs
        TextView logTextView = (TextView) rootView.findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(WifiDirectHandler.LOG_TAG)){
                    // Removes log tag and PID from the log line
                    log.append(line.substring(line.indexOf(": ") + 2)).append("\n");
                }
            }


            this.log.append(log.toString().replace(this.log.toString(), ""));
            //Runtime.getRuntime().exec("logcat -c");
            logTextView.setText(this.log.toString());
        } catch (IOException e) {
        }

        // Creates and returns the AlertDialog for the logs
        AlertDialog.Builder dialogBuilder =  new  AlertDialog.Builder(getActivity())
            .setTitle(getString(R.string.title_logs))
            .setNegativeButton(getString(R.string.action_close),
                new DialogInterface.OnClickListener() {
                    /**
                     * Closes the Dialog when the Close Button is tapped
                     */
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }
            ).setView(rootView);
        return dialogBuilder.create();
    }
}
