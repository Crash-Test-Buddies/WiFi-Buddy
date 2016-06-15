package edu.rit.se.crashavoidance.views;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.wifibuddy.WifiDirectHandler;

/**
 * DialogFragment that shows the WifiDirectHandler log messages
 */
public class LogsDialogFragment extends DialogFragment {

    private StringBuilder log = new StringBuilder();
    private static final String TAG = WifiDirectHandler.TAG + "LogsDialog";

    /**
     * Creates the AlertDialog, sets the WifiDirectHandler instance, and sets the logs TextView
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Sets the Layout for the UI
        LayoutInflater i = getActivity().getLayoutInflater();
        View rootView = i.inflate(R.layout.fragment_logs_dialog, null);

        TextView logTextView = (TextView) rootView.findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(WifiDirectHandler.TAG)){
                    // Removes log tag and PID from the log line
                    log.append(line.substring(line.indexOf(": ") + 2)).append("\n");
                }
            }

            this.log.append(log.toString().replace(this.log.toString(), ""));
            //Runtime.getRuntime().exec("logcat -c");
            logTextView.setText(this.log.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failure reading logcat");
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
