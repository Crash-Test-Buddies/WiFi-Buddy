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

public class LogsDialogFragment extends DialogFragment {

    private TextView logTextView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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
        View rootView = i.inflate(R.layout.fragment_logs_dialog,null);

        logTextView =  (TextView) rootView.findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(getString(R.string.log_tag))){
                    // Removes log tag and PID from the log line
                    log.append(line.substring(line.indexOf(": ") + 2) + "\n");
                }
            }
            logTextView.setText(log.toString());
        } catch (IOException e) {
        }

        dialogBuilder.setView(rootView);
        return dialogBuilder.create();
    }
}
