package edu.rit.se.crashavoidance;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import edu.rit.se.crashavoidance.views.AvailableServicesActivity;

public class initActivity extends AppCompatActivity {

    WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        Toolbar toolbar = (Toolbar) findViewById(R.id.initToolbar);
        setSupportActionBar(toolbar);

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.init_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                onClickMenuDisconnect(item);
                return true;
            case R.id.action_toggle_wifi:
                onClickMenuToggleWifi(item);
                return true;
            case R.id.action_view_logs:
                onClickMenuViewLogs(item);
                return true;
            case R.id.action_exit:
                onClickMenuExit(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickButtonToggleWifi(View view) {
        toggleWifi();
    }

    public void onClickButtonCreateService(View view) {
        createService();
    }

    public void onClickButtonScanServices(View view) {
        scanForServices();
    }

    public void onClickMenuDisconnect(MenuItem item) {
        displayToast("Disconnect tapped");
    }

    public void onClickMenuToggleWifi(MenuItem item) {
        toggleWifi();
    }

    public void onClickMenuViewLogs(MenuItem item) {
        displayToast("View Logs tapped");
    }

    public void onClickMenuExit(MenuItem item) {
        finish();
    }

    private void toggleWifi(){
        if(wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(false);
            displayToast(getString(R.string.status_wifi_disabled));
        } else {
            wifiManager.setWifiEnabled(true);
            displayToast(getString(R.string.status_wifi_enabled));
        }
    }

    private void createService(){
        displayToast("Create Service tapped");
    }

    private void scanForServices(){
        Intent intent = new Intent(this, AvailableServicesActivity.class);
        startActivity(intent);
    }

    public void displayToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
