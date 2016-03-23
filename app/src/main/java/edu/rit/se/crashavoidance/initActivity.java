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
import android.widget.Button;
import android.widget.Toast;

import edu.rit.se.crashavoidance.views.AvailableServicesActivity;
import edu.rit.se.crashavoidance.views.LogsActivity;

public class initActivity extends AppCompatActivity {

    WifiManager wifiManager;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        Toolbar toolbar = (Toolbar) findViewById(R.id.initToolbar);
        setSupportActionBar(toolbar);

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        Button toggleWifiButton = (Button) findViewById(R.id.wifiToggle_btn);
        if(wifiManager.isWifiEnabled()){
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
        } else {
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Adds Main Menu to the ActionBar
        getMenuInflater().inflate(R.menu.main_menu, menu);

        this.menu = menu;

        MenuItem toggleWifiMenuItem = menu.findItem(R.id.action_toggle_wifi);
        if(wifiManager.isWifiEnabled()){
            toggleWifiMenuItem.setTitle(getString(R.string.action_disable_wifi));
        } else {
            toggleWifiMenuItem.setTitle(getString(R.string.action_enable_wifi));
        }

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
        // Open the View Logs Activity
        Intent intent = new Intent(this, LogsActivity.class);
        startActivity(intent);
    }

    public void onClickMenuExit(MenuItem item) {
        finish();
    }

    private void toggleWifi(){
        Button toggleWifiButton = (Button) findViewById(R.id.wifiToggle_btn);
        MenuItem toggleWifiMenuItem = menu.findItem(R.id.action_toggle_wifi);
        if(wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(false);
            displayToast(getString(R.string.status_wifi_disabled));
            toggleWifiButton.setText(getString(R.string.action_enable_wifi));
            toggleWifiMenuItem.setTitle(getString(R.string.action_enable_wifi));
        } else {
            wifiManager.setWifiEnabled(true);
            displayToast(getString(R.string.status_wifi_enabled));
            toggleWifiButton.setText(getString(R.string.action_disable_wifi));
            toggleWifiMenuItem.setTitle(getString(R.string.action_disable_wifi));
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
