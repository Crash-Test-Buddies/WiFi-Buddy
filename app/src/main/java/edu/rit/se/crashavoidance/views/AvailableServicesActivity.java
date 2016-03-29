package edu.rit.se.crashavoidance.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.WiFiP2pService;

public class AvailableServicesActivity extends AppCompatActivity {

    public void onServiceClick(WiFiP2pService service) {
        //TODO: call actual connection logic.
        Intent intent = new Intent(this, GroupMessageActivity.class);
        //TODO: pass data between the activities?
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_services);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setServiceList();
    }

    //TODO: There will need to be a way to dynamically add/remove services
    private void setServiceList(){
        List<WiFiP2pService> services = new ArrayList<WiFiP2pService>();
        // TODO populate off of service detected

        WiFiP2pService s1 = new WiFiP2pService();
        s1.instanceName = "instance name";
        s1.serviceRegistrationType = "registration type";

        services.add(s1);

        ListView listView = (ListView) findViewById(R.id.availableServicesList);
        listView.setAdapter(new AvailableServicesListViewAdapter(this, services));
    }
}
