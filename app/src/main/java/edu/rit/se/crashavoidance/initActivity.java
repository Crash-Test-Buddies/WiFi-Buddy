package edu.rit.se.crashavoidance;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class initActivity extends AppCompatActivity {

    WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);



        findViewById(R.id.wifiToggle_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleWifi();
            }
        });

        findViewById(R.id.createService_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createService();
            }
        });

        findViewById(R.id.scanForServices_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanForServices();
            }
        });

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

    }


    private void toggleWifi(){
        if(wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(false);
        } else {
            wifiManager.setWifiEnabled(true);
        }
    }

    private void createService(){

    }

    private void scanForServices(){

    }
}
