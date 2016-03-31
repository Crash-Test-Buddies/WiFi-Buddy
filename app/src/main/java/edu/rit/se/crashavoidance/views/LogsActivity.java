package edu.rit.se.crashavoidance.views;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.rit.se.crashavoidance.R;

public class LogsActivity extends AppCompatActivity {

    private String log;
    private TextView logTextView;
    private Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        logTextView = (TextView) findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        Log.i("TEST-TAG", "Viewing Logs");
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log=new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("TEST-TAG")){
                    log.append(line);
                }
            }
            logTextView.setText(log.toString());
        } catch (IOException e) {
        }

//        Intent intent = getIntent();
//        String log = intent.getStringExtra(initActivity.EXTRA_LOG);
//
//        logTextView.setText(log);
    }

}
